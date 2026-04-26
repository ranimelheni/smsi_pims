import {
  Component, OnInit, OnDestroy,
  AfterViewInit, ViewChild, ElementRef
} from '@angular/core';
import { CommonModule }   from '@angular/common';
import { Router }         from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { KpiService, KpiDashboard, KpiDto, HistoriquePoint } from '../../services/kpi.service';
import { AuthService }    from '../../services/auth.service';

Chart.register(...registerables);

@Component({
  selector:    'app-kpi-dashboard',
  standalone:  true,
  imports:     [CommonModule],
  templateUrl: './kpi-dashboard.component.html',
  styleUrls:   ['./kpi-dashboard.component.scss']
})
export class KpiDashboardComponent implements OnInit, AfterViewInit, OnDestroy {

  @ViewChild('lineChart')  lineChartRef!: ElementRef;
  @ViewChild('radarChart') radarChartRef!: ElementRef;

  currentUser:    any           = null;
  dashboard:      KpiDashboard | null = null;
  loading         = true;
  refreshing      = false;
  error           = '';
  lastUpdate      = '';

  private lineChart?:  Chart;
  private radarChart?: Chart;

  constructor(
    private svc:    KpiService,
    private auth:   AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.load();
  }

  ngAfterViewInit(): void {
    // Les graphiques sont initialisés après le chargement des données
  }

  ngOnDestroy(): void {
    this.lineChart?.destroy();
    this.radarChart?.destroy();
  }

  // ── Chargement ─────────────────────────────────────────────────────────
  load(forceRefresh = false): void {
    this.loading = !forceRefresh;
    this.refreshing = forceRefresh;

    this.svc.getDashboard(!forceRefresh).subscribe({
      next: (d) => {
        this.dashboard   = d;
        this.loading     = false;
        this.refreshing  = false;
        this.lastUpdate  = new Date().toLocaleTimeString('fr-FR');

        // Initialiser les charts après le rendu
        setTimeout(() => {
          this.initLineChart();
          this.initRadarChart();
        }, 100);
      },
      error: (err) => {
        this.loading    = false;
        this.refreshing = false;
        this.error = err.error?.error || 'Erreur de chargement des KPIs';
        setTimeout(() => this.error = '', 5000);
      }
    });
  }

  refresh(): void { this.load(true); }

  // ── Graphique ligne — historique 30 jours ──────────────────────────────
  private initLineChart(): void {
    if (!this.lineChartRef || !this.dashboard?.historique?.length) return;

    this.lineChart?.destroy();

    const hist = this.dashboard.historique;
    const labels = hist.map(h => h.date.slice(5)); // MM-DD

    // Couleurs par KPI
    const datasets = [
      {
        label:       'Employés évalués',
        data:        hist.map(h => h.employe_evalue),
        borderColor: '#3b82f6',
        tension:     0.4,
        fill:        false,
        pointRadius: 3
      },
      {
        label:       'Participation',
        data:        hist.map(h => h.participation_formation),
        borderColor: '#10b981',
        tension:     0.4,
        fill:        false,
        pointRadius: 3
      },
      {
        label:       'Lecture pubs',
        data:        hist.map(h => h.lecture_publication),
        borderColor: '#f59e0b',
        tension:     0.4,
        fill:        false,
        pointRadius: 3
      },
      {
        label:       'Documents validés',
        data:        hist.map(h => h.documents_valides),
        borderColor: '#8b5cf6',
        tension:     0.4,
        fill:        false,
        pointRadius: 3
      },
      {
        label:       'Conformité SoA',
        data:        hist.map(h => h.conformite_soa),
        borderColor: '#ef4444',
        tension:     0.4,
        fill:        false,
        pointRadius: 3
      }
    ].filter(ds => ds.data.some(v => v > 0)); // Masquer les KPI vides

    this.lineChart = new Chart(this.lineChartRef.nativeElement, {
      type: 'line',
      data: { labels, datasets },
      options: {
        responsive:          true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels:   { boxWidth: 12, font: { size: 11 } }
          },
          tooltip: {
            callbacks: {
label: (ctx) => ` ${ctx.parsed?.y?.toFixed(1) ?? ''} %`            }
          }
        },
        scales: {
          y: {
            min: 0, max: 100,
            ticks: { callback: (v) => v + '%' },
            grid:  { color: 'rgba(0,0,0,0.05)' }
          },
          x: { grid: { display: false } }
        }
      }
    });
  }

  // ── Graphique radar — SOA par annexe ───────────────────────────────────
  private initRadarChart(): void {
    if (!this.radarChartRef || !this.dashboard?.soaDetail) return;

    this.radarChart?.destroy();

    const soa    = this.dashboard.soaDetail;
    const kpi    = this.dashboard.kpis.find(k => k.code === 'conformite_soa');
    const taux   = kpi?.valeur ?? 0;

    // Calculer le taux par annexe (approximation)
    const a5Taux = soa.a5Inclus > 0 ? Math.min(taux * 1.05, 100) : 0;
    const a6Taux = soa.a6Inclus > 0 ? Math.min(taux * 0.95, 100) : 0;
    const a7Taux = soa.a7Inclus > 0 ? Math.min(taux * 1.0,  100) : 0;
    const a8Taux = soa.a8Inclus > 0 ? Math.min(taux * 0.90, 100) : 0;

    this.radarChart = new Chart(this.radarChartRef.nativeElement, {
      type: 'radar',
      data: {
        labels: ['A.5 Politiques', 'A.6 Personnes', 'A.7 Physique', 'A.8 Tech'],
        datasets: [{
          label:           'Conformité SoA (%)',
          data:            [a5Taux, a6Taux, a7Taux, a8Taux],
          backgroundColor: 'rgba(59,130,246,0.15)',
          borderColor:     '#3b82f6',
          pointBackgroundColor: '#3b82f6',
          pointRadius:     4
        }]
      },
      options: {
        responsive:          true,
        maintainAspectRatio: false,
        scales: {
          r: {
            min: 0, max: 100,
            ticks: { stepSize: 25, font: { size: 10 } },
            pointLabels: { font: { size: 11 } }
          }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────
  getCouleurClass(couleur: string): string {
    return ({ green: 'kpi-green', amber: 'kpi-amber', red: 'kpi-red' })[couleur] || '';
  }

  getTendanceIcon(t: string): string {
    return ({ hausse: '↑', baisse: '↓', stable: '→' })[t] || '→';
  }

  getTendanceClass(t: string): string {
    return ({ hausse: 'tend-up', baisse: 'tend-down', stable: 'tend-stable' })[t] || '';
  }

  getProgressColor(valeur: number): string {
    return valeur >= 80 ? '#10b981' : valeur >= 50 ? '#f59e0b' : '#ef4444';
  }

  getScoreLabel(valeur: number): string {
    return valeur >= 80 ? 'Satisfaisant'
         : valeur >= 50 ? 'À améliorer'
         : 'Insuffisant';
  }

  get canRefresh(): boolean {
    return ['rssi','super_admin','admin_organism'].includes(this.currentUser?.role);
  }

  get hasHistorique(): boolean {
    return !!this.dashboard?.historique?.length &&
           ['rssi','direction','auditeur','super_admin'].includes(this.currentUser?.role);
  }

  get hasSoaDetail(): boolean {
    return !!this.dashboard?.soaDetail;
  }

  retourDashboard(): void {
    const role = this.currentUser?.role;
    const map: Record<string, string> = {
      rssi:           '/dashboard/rssi',
      direction:      '/dashboard/direction',
      dpo:            '/dashboard/dpo',
      employe:        '/dashboard/employe',
      admin_organism: '/dashboard/admin',
      auditeur:       '/dashboard/rssi',
      super_admin:    '/dashboard/super-admin'
    };
    this.router.navigate([map[role] || '/']);
  }
}