package platform.disl.parser;

import java.util.Objects;
import java.util.Optional;

/**
 * One parsed KZ dislocation line (20 fields, or 21 with containers).
 * Field meanings match {@code ImportStateReportsFtp.SaveStateReportLine} / 01_ASUVP_IMPORT_PIPELINE.md.
 */
public final class KzDislocationLine {

    private final String wagonCode;
    private final String dislocationStationCode;
    private final String trainIndex;
    private final String dislocationDate;
    private final String operationCode;
    private final String destinationStationCode;
    private final String departureStationCode;
    private final String receiverCode;
    private final String senderCode;
    private final String wagonType;
    private final int wagonOwner;
    private final String nodCode;
    private final String weigthState;
    private final String workState;
    private final String docNumber;
    private final String cargoCode;
    private final String cargoWeigth;
    private final String capacity;
    private final String messageCode;
    private final String operationDate;
    private final String containers;
    private final int fieldCount;
    private final String rawLine;

    public KzDislocationLine(
            String wagonCode,
            String dislocationStationCode,
            String trainIndex,
            String dislocationDate,
            String operationCode,
            String destinationStationCode,
            String departureStationCode,
            String receiverCode,
            String senderCode,
            String wagonType,
            int wagonOwner,
            String nodCode,
            String weigthState,
            String workState,
            String docNumber,
            String cargoCode,
            String cargoWeigth,
            String capacity,
            String messageCode,
            String operationDate,
            String containers,
            int fieldCount,
            String rawLine) {
        this.wagonCode = Objects.requireNonNull(wagonCode, "wagonCode");
        this.dislocationStationCode = Objects.requireNonNull(dislocationStationCode, "dislocationStationCode");
        this.trainIndex = Objects.requireNonNull(trainIndex, "trainIndex");
        this.dislocationDate = Objects.requireNonNull(dislocationDate, "dislocationDate");
        this.operationCode = Objects.requireNonNull(operationCode, "operationCode");
        this.destinationStationCode = Objects.requireNonNull(destinationStationCode, "destinationStationCode");
        this.departureStationCode = Objects.requireNonNull(departureStationCode, "departureStationCode");
        this.receiverCode = Objects.requireNonNull(receiverCode, "receiverCode");
        this.senderCode = Objects.requireNonNull(senderCode, "senderCode");
        this.wagonType = Objects.requireNonNull(wagonType, "wagonType");
        this.wagonOwner = wagonOwner;
        this.nodCode = Objects.requireNonNull(nodCode, "nodCode");
        this.weigthState = Objects.requireNonNull(weigthState, "weigthState");
        this.workState = Objects.requireNonNull(workState, "workState");
        this.docNumber = Objects.requireNonNull(docNumber, "docNumber");
        this.cargoCode = Objects.requireNonNull(cargoCode, "cargoCode");
        this.cargoWeigth = Objects.requireNonNull(cargoWeigth, "cargoWeigth");
        this.capacity = Objects.requireNonNull(capacity, "capacity");
        this.messageCode = Objects.requireNonNull(messageCode, "messageCode");
        this.operationDate = Objects.requireNonNull(operationDate, "operationDate");
        this.containers = containers;
        this.fieldCount = fieldCount;
        this.rawLine = Objects.requireNonNull(rawLine, "rawLine");
    }

    public String wagonCode() {
        return wagonCode;
    }

    public String dislocationStationCode() {
        return dislocationStationCode;
    }

    public String trainIndex() {
        return trainIndex;
    }

    public String dislocationDate() {
        return dislocationDate;
    }

    public String operationCode() {
        return operationCode;
    }

    public String destinationStationCode() {
        return destinationStationCode;
    }

    public String departureStationCode() {
        return departureStationCode;
    }

    public String receiverCode() {
        return receiverCode;
    }

    public String senderCode() {
        return senderCode;
    }

    public String wagonType() {
        return wagonType;
    }

    public int wagonOwner() {
        return wagonOwner;
    }

    public String nodCode() {
        return nodCode;
    }

    public String weigthState() {
        return weigthState;
    }

    public String workState() {
        return workState;
    }

    public String docNumber() {
        return docNumber;
    }

    public String cargoCode() {
        return cargoCode;
    }

    public String cargoWeigth() {
        return cargoWeigth;
    }

    public String capacity() {
        return capacity;
    }

    public String messageCode() {
        return messageCode;
    }

    public String operationDate() {
        return operationDate;
    }

    /** Present only when the source line had exactly 21 fields. */
    public Optional<String> containers() {
        return Optional.ofNullable(containers);
    }

    public int fieldCount() {
        return fieldCount;
    }

    public String rawLine() {
        return rawLine;
    }
}
