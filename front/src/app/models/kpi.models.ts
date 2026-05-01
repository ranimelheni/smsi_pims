export interface KpiResponse {
  organism_id:    number;
  organism_nom:   string;
  computed_at:    string;
  has_data:       boolean;
  message_vide:   string | null;
  soa:            SoaKpi | null;
  publications:   PublicationKpi | null;
}

export interface SoaKpi {
  has_data:         boolean;
  taux_global:      number;
  total_controles:  number;
  nb_implemente:    number;
  nb_en_cours:      number;
  nb_non_commence:  number;
  par_annexe:       SoaParAnnexe[];
  evolution:        SoaEvolutionPoint[];
}

export interface SoaParAnnexe {
  annexe:        string;
  annexe_label:  string;
  total_inclus:  number;
  nb_implemente: number;
  taux_annexe:   number;
}

export interface SoaEvolutionPoint {
  date:          string;
  taux_soa:      number;
  nb_implemente: number;
  total_inclus:  number;
}

export interface PublicationKpi {
  has_data:              boolean;
  total_publications:    number;
  total_destinataires:   number;
  nb_lecteurs_uniques:   number;
  taux_lecture_global:   number;
  par_type:              PubParType[];
  par_priorite:          PubParPriorite[];
  evolution_mensuelle:   PubEvolutionMensuelle[];
}

export interface PubParType {
  type_publication: string;
  nb_publications:  number;
  nb_lectures:      number;
  taux_lecture:     number;
}

export interface PubParPriorite {
  priorite:        string;
  nb_publications: number;
  nb_lecteurs:     number;
  taux_lecture:    number;
}

export interface PubEvolutionMensuelle {
  mois:                string;
  nb_publications:     number;
  nb_lectures:         number;
  nb_lecteurs_uniques: number;
}