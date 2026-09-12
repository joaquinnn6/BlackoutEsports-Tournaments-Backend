package cl.duocuc.blackout.tournaments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tournaments", uniqueConstraints = @UniqueConstraint(columnNames = {"name_key", "game_key"}))
@Getter
@Setter
@NoArgsConstructor
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 60)
    private String game;
    @Column(nullable = false, length = 40)
    private String status;
    @Column(name = "display_date", nullable = false, length = 80)
    private String date;
    @Column(nullable = false, length = 80)
    private String teams;
    @Column(name = "name_key", nullable = false, length = 120)
    private String nameKey;
    @Column(name = "game_key", nullable = false, length = 60)
    private String gameKey;
}
