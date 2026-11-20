package platform.rwl.proto.parse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import platform.rwl.proto.model.RwlOperation;

/**
 * Parses documented RWL XML responses. Does not invent a JSON mapping.
 */
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

    public ParseOutcome parse(String body, Instant fetchedAtUtc) {
        if (body == null || body.isBlank()) {
            return ParseOutcome.parseError("empty_response_body");
        }

        String trimmed = body.trim();
        String apiError = detectPlainApiError(trimmed);
        if (apiError != null) {
            return ParseOutcome.apiError(apiError);
        }

        if (!trimmed.startsWith("<")) {
            return ParseOutcome.parseError("non_xml_response");
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
            return ParseOutcome.parseError("xml_parse_failed: " + e.getMessage());
        }

        Element root = doc.getDocumentElement();
        if (root == null) {
            return ParseOutcome.parseError("missing_document_element");
        }

        String rootError = text(root, "error");
        if (rootError != null && !rootError.isBlank()) {
            return ParseOutcome.apiError(rootError.trim());
        }
        if ("error".equalsIgnoreCase(root.getTagName()) && root.getTextContent() != null) {
            return ParseOutcome.apiError(root.getTextContent().trim());
        }

        List<Element> vagons = elementsByTag(root, "vagon");
        if (vagons.isEmpty() && "vagon".equalsIgnoreCase(root.getTagName())) {
            vagons = List.of(root);
        }

        ParseOutcome.Builder builder = new ParseOutcome.Builder();
        for (Element vagon : vagons) {
            builder.addWagon();
            parseVagon(vagon, fetchedAtUtc, builder);
        }

        // Unknown structure: XML but no vagon nodes and not an empty list wrapper we understand
        if (vagons.isEmpty()) {
            String rootName = root.getTagName().toLowerCase(Locale.ROOT);
            if (rootName.contains("vagons") || rootName.equals("xml") || rootName.equals("data")
                    || rootName.equals("response") || rootName.equals("result")) {
                return builder.buildSuccessOrEmpty();
            }
            return ParseOutcome.parseError("unknown_xml_structure root=" + root.getTagName());
        }

        return builder.buildSuccessOrEmpty();
    }

    private void parseVagon(Element vagon, Instant fetchedAtUtc, ParseOutcome.Builder builder) {
        Element info = firstChild(vagon, "vagon_info");
        String wagonNo = info == null ? text(vagon, "vagon_no") : text(info, "vagon_no");
        String fromCode = info == null ? null : text(info, "from_code");
        String fromName = info == null ? null : text(info, "from_name");
        String toCode = info == null ? null : text(info, "to_code");
        String toName = info == null ? null : text(info, "to_name");

        List<Element> positions = elementsByTag(vagon, "position");
        if (positions.isEmpty()) {
            builder.addReject(
                    wagonNo,
                    "no_position",
                    "vagon without position",
                    excerpt(vagon));
            return;
        }

        for (Element position : positions) {
            LocalDateTime utc = parseUtcDatetime(text(position, "utc_datetime"));
            String stationCode = text(position, "current_position_code");
            String stationName = text(position, "current_position");
            String operationCode = text(position, "operation_code");
            String asoup = text(position, "operation_asoup_code");
            String operationName = text(position, "operation");
            String operationTypeId = text(position, "operation_id");
            String trainIndex = text(position, "train_index");
            String trainNumber = text(position, "train_number");
            String loaded = text(position, "loaded");
            String cargo = text(position, "cargo_name");
            String etsng = text(position, "etsng_code");
            String weight = text(position, "weight");
            String lat = text(position, "current_position_latitude");
            String lon = text(position, "current_position_longitude");

            // Trip stations from vagon_info; operation-specific from_station/to_station remain UNKNOWN preference
            RwlOperation op = new RwlOperation(
                    wagonNo,
                    stationCode,
                    stationName,
                    utc,
                    operationCode,
                    asoup,
                    operationName,
                    operationTypeId,
                    trainIndex,
                    trainNumber,
                    fromCode,
                    fromName,
                    toCode,
                    toName,
                    loaded,
                    cargo,
                    etsng,
                    weight,
                    lat,
                    lon,
                    fetchedAtUtc);

            if (op.eventKey().isEmpty()) {
                builder.addReject(
                        wagonNo,
                        op.missingIdentityReason(),
                        "cannot build event_key",
                        excerpt(position));
            } else {
                builder.addOperation(op);
            }
        }
    }

    static LocalDateTime parseUtcDatetime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim();
        try {
            if (v.contains("T")) {
                return LocalDateTime.parse(v);
            }
            return LocalDateTime.parse(v, UTC_DATETIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String detectPlainApiError(String body) {
        String lower = body.toLowerCase(Locale.ROOT);
        if (body.length() < 200 && !body.contains("<")) {
            for (String known : KNOWN_API_ERRORS) {
                if (lower.contains(known.toLowerCase(Locale.ROOT))) {
                    return body.trim();
                }
            }
            // short plain-text often used for auth errors
            if (lower.contains("incorrect") || lower.contains("error") || lower.contains("wrong_")) {
                return body.trim();
            }
        }
        return null;
    }

    private static Element firstChild(Element parent, String tag) {
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && tag.equalsIgnoreCase(n.getNodeName())) {
                return (Element) n;
            }
        }
        return null;
    }

    private static List<Element> elementsByTag(Element root, String tag) {
        NodeList list = root.getElementsByTagName(tag);
        List<Element> out = new ArrayList<>();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                out.add((Element) n);
            }
        }
        // de-dupe while preserving order (getElementsByTagName is fine for position under vagon)
        return new ArrayList<>(new LinkedHashSet<>(out));
    }

    private static String text(Element parent, String tag) {
        if (parent == null) {
            return null;
        }
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            return null;
        }
        // Prefer direct child
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getParentNode() == parent) {
                String v = n.getTextContent();
                return v == null ? null : v.trim();
            }
        }
        String v = list.item(0).getTextContent();
        return v == null ? null : v.trim();
    }

    private static String excerpt(Element el) {
        String xml;
        try {
            xml = el.getTextContent();
        } catch (Exception e) {
            return "";
        }
        if (xml == null) {
            return "";
        }
        String oneLine = xml.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 400 ? oneLine.substring(0, 400) : oneLine;
    }

    public static String readUtf8(byte[] bytes) throws IOException {
        if (bytes == null) {
            throw new IOException("null body");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
