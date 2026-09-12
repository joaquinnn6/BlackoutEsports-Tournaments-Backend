package cl.duocuc.blackout.tournaments;

public record TournamentResponse(Long id, String name, String game, String status, String date, String teams) {
    public static TournamentResponse from(Tournament tournament) {
        return new TournamentResponse(tournament.getId(), tournament.getName(), tournament.getGame(),
                tournament.getStatus(), tournament.getDate(), tournament.getTeams());
    }
}
