// ErmSocieteMission.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_societe_mission")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmSocieteMission {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false, unique = true)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(length = 255) private String nomSociete;
    @Column(length = 255) private String adresse;
    @Column(length = 255) private String contact;
    @Column(columnDefinition = "TEXT") private String mission;
}