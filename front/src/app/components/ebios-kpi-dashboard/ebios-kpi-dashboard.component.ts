import {
  Component, Input, OnInit, OnChanges,
  SimpleChanges, AfterViewInit, OnDestroy,
  ViewChild, ElementRef, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient }   from '@angular/common/http';

// Import Chart.js uniquement les éléments nécessaires
import {
  Chart,
  DoughnutController, BarController,
  ArcElement, BarElement,
  CategoryScale, LinearScale,
  Tooltip, Legend
} from 'chart.js';

Chart.register(
  DoughnutController, BarController,
  ArcElement, BarElement,
  CategoryScale, LinearScale,
  Tooltip, Legend
);

interface EbiosKpi {
  analyseId:          number;
  analyseTitre:       string;
  analyseStatut:      string;
  nbValeursMetier:    number;
  nbBiensSupport:     number;
  nbEvenements:       number;
  nbSourcesRisque:    number;
  nbScenariosStrat:   number;
  nbScenariosOp:      number;
  nbMesures:          number;
  nbResiduels:        number;
  kpiCouverture:      number;
  couvertureNum:      number;
  couvertureDen:      number;
  couvertureAcceptes: number;
  kpiNiveauResiduel:  number;
  residuelTotal:      number;
  residuelEleve:      number;
  residuelMoyen:      number;
  residuelFaible:     number;
  kpiMesures:         number;
  mesuresNum:         number;
  mesuresDen:         number;
  mesuresEnCours:     number;
  mesuresPlanifiees:  number;
  niveauGlobal:       string;
  couleurGlobale:     string;
  scoreCompletion:    number;
}

@Component({
  selector:    'app-ebios-kpi-dashboard',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './ebios-kpi-dashboard.component.html',
  styleUrls:   ['./ebios-kpi-dashboard.component.scss']
})
export class EbiosKpiDashboardComponent
  implements OnInit, OnChanges, AfterViewInit, OnDestroy {

  @Input() analyseId!: number;

  @ViewChild('doughnutRef') doughnutRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('barRef')      barRef!:      ElementRef<HTMLCanvasElement>;

  kpi:      EbiosKpi | null = null;
  loading   = true;
  error     = '';

  private doughnutChart?: Chart;
  private barChart?:      Chart;
  private chartsReady     = false;

  private readonly api = 'http://localhost:8080/api/ebios/kpi';

  constructor(
    private http: HttpClient,
    private cdr:  ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.analyseId) this.loadKpi();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['analyseId'] && !changes['analyseId'].firstChange) {
      this.destroyCharts();
      this.loadKpi();
    }
  }

  ngAfterViewInit(): void {
    this.chartsReady = true;
    // Si les données sont déjà chargées, initialiser les charts
    if (this.kpi && !this.loading) {
      setTimeout(() => this.initCharts(), 0);
    }
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  // ── Chargement ────────────────────────────────────────────────────────────
  loadKpi(): void {
    this.loading = true;
    this.error   = '';

    this.http.get<EbiosKpi>(`${this.api}/${this.analyseId}`).subscribe({
      next: (data) => {
        this.kpi     = data;
        this.loading = false;
        this.cdr.detectChanges();

        // Initialiser les charts seulement si la vue est prête
        if (this.chartsReady) {
          setTimeout(() => this.initCharts(), 50);
        }
      },
      error: (err) => {
        // Ne pas bloquer — afficher message discret
        this.error   = err.status === 404
          ? 'Aucune donnée disponible pour cette analyse.'
          : 'Erreur de chargement des indicateurs.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Charts ────────────────────────────────────────────────────────────────
  private initCharts(): void {
    if (!this.kpi) return;
    this.initDoughnut();
    this.initBar();
  }

  private initDoughnut(): void {
    if (!this.doughnutRef?.nativeElement) return;
    this.doughnutChart?.destroy();

    const kpi = this.kpi!;
    const ctx  = this.doughnutRef.nativeElement.getContext('2d');
    if (!ctx) return;

    // Doughnut : répartition des risques résiduels
    this.doughnutChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels:   ['Élevé', 'Moyen', 'Faible'],
        datasets: [{
          data: [
            kpi.residuelEleve,
            kpi.residuelMoyen,
            kpi.residuelFaible
          ],
          backgroundColor: ['#ef4444', '#f59e0b', '#10b981'],
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive:          true,
        maintainAspectRatio: false,
        cutout:              '68%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              boxWidth: 10,
              font:     { size: 11 },
              padding:  8
            }
          },
          tooltip: {
            callbacks: {
              label: (ctx) =>
                ` ${ctx.label} : ${ctx.parsed} scénario(s)`
            }
          }
        },
        // IMPORTANT : désactiver les animations pour éviter les blocages
        animation: { duration: 300 }
      }
    });
  }

  private initBar(): void {
    if (!this.barRef?.nativeElement) return;
    this.barChart?.destroy();

    const kpi = this.kpi!;
    const ctx  = this.barRef.nativeElement.getContext('2d');
    if (!ctx) return;

    // Bar : état des mesures de sécurité
    this.barChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['Réalisées', 'En cours', 'Planifiées'],
        datasets: [{
          label:           'Mesures de sécurité',
          data: [
            kpi.mesuresNum,
            kpi.mesuresEnCours,
            kpi.mesuresPlanifiees
          ],
          backgroundColor: ['#10b981', '#3b82f6', '#9ca3af'],
          borderRadius:    6,
          borderWidth:     0
        }]
      },
      options: {
        responsive:          true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${ctx.parsed.y} mesure(s)`
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { font: { size: 11 } }
          },
          y: {
            beginAtZero: true,
            ticks: {
              stepSize: 1,
              font:     { size: 11 }
            },
            grid: { color: 'rgba(0,0,0,0.05)' }
          }
        },
        animation: { duration: 300 }
      }
    });
  }

  private destroyCharts(): void {
    this.doughnutChart?.destroy();
    this.barChart?.destroy();
    this.doughnutChart = undefined;
    this.barChart      = undefined;
  }

  // ── Helpers affichage ─────────────────────────────────────────────────────
  getCouleurClass(c: string): string {
    return ({
      green:  'niveau-green',
      amber:  'niveau-amber',
      orange: 'niveau-orange',
      red:    'niveau-red',
      gray:   'niveau-gray'
    })[c] || 'niveau-gray';
  }

  getProgressColor(valeur: number): string {
    return valeur >= 80 ? '#10b981' : valeur >= 50 ? '#f59e0b' : '#ef4444';
  }

  getNiveauResiduelLabel(v: number): string {
    if (v === 0)   return 'Non évalué';
    if (v >= 3.5)  return 'Critique';
    if (v >= 2.5)  return 'Élevé';
    if (v >= 1.5)  return 'Moyen';
    return 'Faible';
  }

  getNiveauResiduelColor(v: number): string {
    if (v === 0)   return '#9ca3af';
    if (v >= 3.5)  return '#dc2626';
    if (v >= 2.5)  return '#ef4444';
    if (v >= 1.5)  return '#f59e0b';
    return '#10b981';
  }
}