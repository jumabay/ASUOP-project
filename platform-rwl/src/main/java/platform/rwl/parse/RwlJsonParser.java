package platform.rwl.parse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import platform.intake.IntakeReject;

/**
 * Provisional JSON parser for synthetic fixtures that mirror XML field names.
 * Not derived from an official live JSON sample (UNKNOWN until captured).
 */
public final class RwlJsonParser {

    private static final DateTimeFormatter UTC_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Minimal extraction without adding Jackson dependency to this module.
    private static final Pattern VAGON_BLOCK =
            Pattern.compile("\\{[^{}]*\"vagon_no\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}", Pattern.DOTALL);

    public ParseResult parse(String body, Instant fetchedAtUtc) {
        if (body == null || body.isBlank()) {
            return ParseResult.parseError("empty_response_body");
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("<")) {
            return ParseResult.parseError("xml_passed_to_json_parser");
        }
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            if (trimmed.length() < 200 && !trimmed.contains("{")) {
                return ParseResult.apiError("plain_api_error");
            }
            return ParseResult.parseError("non_json_response");
        }

        ParseResult.Builder builder = new ParseResult.Builder();
        List<String> objects = splitTopLevelObjects(trimmed);
        if (objects.isEmpty()) {
            return builder.buildOkOrEmpty();
        }
        for (String obj : objects) {
            builder.addWagon();
            parseObject(obj, fetchedAtUtc, builder);
        }
        return builder.buildOkOrEmpty();
    }

    private void parseObject(String obj, Instant fetchedAtUtc, ParseResult.Builder builder) {
        String wagonNo = stringField(obj, "vagon_no");
        if (wagonNo.isEmpty()) {
            builder.addReject(new IntakeReject("missing_wagon_no", "", "json object without vagon_no"));
            return;
        }
        RawRwlOperation op = new RawRwlOperation();
        op.wagonNo = wagonNo;
        op.stationCode = stringField(obj, "current_position_code");
        op.stationName = stringField(obj, "current_position");
        op.operationCode = stringField(obj, "operation_code");
        op.operationAsoupCode = stringField(obj, "operation_asoup_code");
        op.operationName = stringField(obj, "operation");
        op.trainIndex = stringField(obj, "train_index");
        op.trainNumber = stringField(obj, "train_number");
        op.fromStationCode = stringField(obj, "from_code");
        op.toStationCode = stringField(obj, "to_code");
        op.loadedFlag = stringField(obj, "loaded");
        op.cargoName = stringField(obj, "cargo_name");
        op.etsngCode = stringField(obj, "etsng_code");
        op.weightTons = stringField(obj, "weight");
        op.fetchedAtUtc = fetchedAtUtc;
        String utc = stringField(obj, "utc_datetime");
        if (utc.isEmpty()) {
            builder.addReject(new IntakeReject("missing_utc_datetime", wagonNo, "utc_datetime required"));
            return;
        }
        try {
            op.operationTimeUtc = LocalDateTime.parse(utc, UTC_DATETIME);
        } catch (DateTimeParseException e) {
            builder.addReject(new IntakeReject("bad_utc_datetime", wagonNo, utc));
            return;
        }
        builder.addOperation(op);
    }

    private static List<String> splitTopLevelObjects(String json) {
        List<String> out = new ArrayList<>();
        Matcher m = VAGON_BLOCK.matcher(json);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    private static String stringField(String jsonObject, String field) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(jsonObject);
        return m.find() ? m.group(1).trim() : "";
    }
}
