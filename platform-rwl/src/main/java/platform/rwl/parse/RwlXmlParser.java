package platform.rwl.parse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import platform.intake.IntakeReject;

/** Parses documented RWL XML. Does not open network connections. */
public final class RwlXmlParser {

    private static final DateTimeFormatter UTC_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> KNOWN_API_ERRORS = Set.of(
            "no funds left",
            "incorrect_user_login",
            "no_such_vagon",
            "wrong_vagon_no",
            "wrong_client_id",
            "incorrect_vagon_check",
            "incorrect_length",
            "incorrect group id",
            "incorrect_group_id",
            "wrong_group_id",
            "empty_vagon_no_and_client_id",
            "bad_xml");

    public ParseResult parse(String body, Instant fetchedAtUtc) {
        if (body == null || body.isBlank()) {
            return ParseResult.parseError("empty_response_body");
        }
        String trimmed = body.trim();
        String apiError = detectPlainApiError(trimmed);
        if (apiError != null) {
            return ParseResult.apiError(apiError);
        }
        if (!trimmed.startsWith("<")) {
            return ParseResult.parseError("non_xml_response");
        }

        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return ParseResult.parseError("xml_parse_failed");
        }

        Element root = doc.getDocumentElement();
        if (root == null) {
            return ParseResult.parseError("missing_document_element");
        }
        String rootError = text(root, "error");
        if (rootError != null && !rootError.isBlank()) {
            return ParseResult.apiError(rootError.trim());
        }
        if ("error".equalsIgnoreCase(root.getTagName()) && root.getTextContent() != null) {
            return ParseResult.apiError(root.getTextContent().trim());
        }

        List<Element> vagons = elementsByTag(root, "vagon");
        if (vagons.isEmpty() && "vagon".equalsIgnoreCase(root.getTagName())) {
            vagons = List.of(root);
        }

        ParseResult.Builder builder = new ParseResult.Builder();
        for (Element vagon : vagons) {
            builder.addWagon();
            parseVagon(vagon, fetchedAtUtc, builder);
        }

        if (vagons.isEmpty()) {
            String rootName = root.getTagName().toLowerCase(Locale.ROOT);
            if (rootName.contains("vagons")
                    || rootName.equals("xml")
                    || rootName.equals("data")
                    || rootName.equals("response")
                    || rootName.equals("result")) {
                return builder.buildOkOrEmpty();
            }
            return ParseResult.parseError("unknown_xml_structure");
        }
        return builder.buildOkOrEmpty();
    }

    private void parseVagon(Element vagon, Instant fetchedAtUtc, ParseResult.Builder builder) {
        Element info = firstChild(vagon, "vagon_info");
        Element position = firstChild(vagon, "position");
        String wagonNo = info == null ? text(vagon, "vagon_no") : text(info, "vagon_no");
        if (wagonNo == null || wagonNo.isBlank()) {
            builder.addReject(new IntakeReject("missing_wagon_no", "", "vagon without vagon_no"));
            return;
        }
        if (position == null) {
            builder.addReject(new IntakeReject("missing_position", wagonNo, "no position element"));
            return;
        }

        RawRwlOperation op = new RawRwlOperation();
        op.wagonNo = wagonNo.trim();
        op.fromStationCode = info == null ? "" : nullToEmpty(text(info, "from_code"));
        op.toStationCode = info == null ? "" : nullToEmpty(text(info, "to_code"));
        op.stationCode = nullToEmpty(text(position, "current_position_code"));
        op.stationName = nullToEmpty(text(position, "current_position"));
        op.operationCode = nullToEmpty(text(position, "operation_code"));
        op.operationAsoupCode = nullToEmpty(text(position, "operation_asoup_code"));
        op.operationName = nullToEmpty(text(position, "operation"));
        op.trainIndex = nullToEmpty(text(position, "train_index"));
        op.trainNumber = nullToEmpty(text(position, "train_number"));
        op.loadedFlag = nullToEmpty(text(position, "loaded"));
        op.cargoName = nullToEmpty(text(position, "cargo_name"));
        op.etsngCode = nullToEmpty(text(position, "etsng_code"));
        op.weightTons = nullToEmpty(text(position, "weight"));
        op.fetchedAtUtc = fetchedAtUtc;

        String utc = text(position, "utc_datetime");
        if (utc == null || utc.isBlank()) {
            builder.addReject(new IntakeReject("missing_utc_datetime", wagonNo, "utc_datetime required"));
            return;
        }
        try {
            op.operationTimeUtc = LocalDateTime.parse(utc.trim(), UTC_DATETIME);
        } catch (DateTimeParseException e) {
            builder.addReject(new IntakeReject("bad_utc_datetime", wagonNo, utc));
            return;
        }
        builder.addOperation(op);
    }

    private static String detectPlainApiError(String trimmed) {
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("<")) {
            return null;
        }
        for (String known : KNOWN_API_ERRORS) {
            if (lower.contains(known)) {
                return known;
            }
        }
        if (!trimmed.contains("<") && trimmed.length() < 200) {
            return "plain_api_error";
        }
        return null;
    }

    private static Element firstChild(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getParentNode() == parent && n instanceof Element el) {
                return el;
            }
        }
        if (nodes.getLength() > 0 && nodes.item(0) instanceof Element el) {
            return el;
        }
        return null;
    }

    private static List<Element> elementsByTag(Element root, String tag) {
        NodeList nodes = root.getElementsByTagName(tag);
        List<Element> out = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element el) {
                out.add(el);
            }
        }
        return out;
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node n = nodes.item(0);
        return n.getTextContent() == null ? null : n.getTextContent().trim();
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v.trim();
    }
}
