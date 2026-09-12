package cl.duocuc.blackout.tournaments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TournamentRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 60) String game,
        @NotBlank @Size(max = 40) String status,
        @NotBlank @Size(max = 80) String date,
        @NotBlank @Size(max = 80) String teams) {
}
