package org.nepe.shared.exception;

/**
 * Thrown during CSV ingestion when an unmapped team name is encountered.
 * <p>
 * This exception carries the raw team name and competition context so that the UI layer
 * can prompt the user with the alias mapping modal dialog.
 */
public class AliasMappingRequiredException extends NepeException {

    private final String rawTeamName;
    private final String competitionCode;

    public AliasMappingRequiredException(String rawTeamName) {
        super(String.format("Unrecognized team name '%s' requires alias mapping.", rawTeamName));
        this.rawTeamName = rawTeamName;
        this.competitionCode = null;
    }

    public AliasMappingRequiredException(String rawTeamName, String competitionCode) {
        super(String.format("Unrecognized team name '%s' in competition '%s' requires alias mapping.", rawTeamName, competitionCode));
        this.rawTeamName = rawTeamName;
        this.competitionCode = competitionCode;
    }

    public String getRawTeamName() {
        return rawTeamName;
    }

    public String getCompetitionCode() {
        return competitionCode;
    }
}
