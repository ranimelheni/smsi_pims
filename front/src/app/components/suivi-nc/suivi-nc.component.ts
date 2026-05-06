import {
  Component, OnInit, OnDestroy,
  NgZone, ChangeDetectorRef, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SuiviNcService } from '../../services/suivi-nc.service';
import { AuthService } from '../../services/auth.service';
import { SuiviNc, SuiviNcKpi } from '../../models/suivi-nc.models';
import {
  Chart, BarController, DoughnutController,
  BarElement, ArcElement,
  CategoryScale, LinearScale,
  Tooltip, Legend
} from 'chart.js';

Chart.register(
  BarController, DoughnutController,
  BarElement, ArcElement,
  CategoryScale, LinearScale,
  Tooltip, Legend
);
Chart.defaults.animation = false as any;
Chart.defaults.responsive = true;
Chart.defaults.maintainAspectRatio = false;

@Component({
  selector: 'app-suivi-nc',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './suivi-nc.component.html',
  styleUrls: ['./suivi-nc.component.scss']
})
export class SuiviNcComponent implements OnInit, OnDestroy {

  currentUser: any = null;
  liste: SuiviNc[]  = [];
  kpi:  SuiviNcKpi | null = null;

  loading      = true;
  loadingKpi   = true;
  saving       = new Set<number>();
  success      = '';
  error        = '';

  filtreStatut   = '';
  filtrePriorite = '';
  filtreClause   = '';
  activeTab: 'liste' | 'kpi' = 'liste';

  private readonly uid = Date.now().toString(36);
  private charts = new Map<string, Chart>();
  private timers: ReturnType<typeof setTimeout>[] = [];

  readonly ids = {
    doughnut: `nc-doughnut-${this.uid}`,
    barClause: `nc-bar-${this.uid}`
  };

  STATUTS_IMPL = [
    { value: 'non_traite', label: 'Non traité',  color: '#9ca3af' },
    { value: 'en_cours',   label: 'En cours',    color: '#f59e0b' },
    { value: 'fait',       label: 'Fait',         color: '#10b981' },
    { value: 'reporte',    label: 'Reporté',      color: '#6b7280' },
    { value: 'accepte',    label: 'Accepté',      color: '#3b82f6' }
  ];

  PRIORITES = ['basse', 'normale', 'haute', 'critique'];

  constructor(
    private svc:  SuiviNcService,
    private auth: AuthService,
    private zone: NgZone,
    private cdr:  ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.loadListe();
    this.loadKpi();
  }

  ngOnDestroy(): void {
    this.timers.forEach(t => clearTimeout(t));
    this.charts.forEach(c => { try { c.destroy(); } catch (_) {} });
  }

  loadListe(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.svc.getListe().subscribe({
      next: (data) => {
        this.liste   = data;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.loading = false; this.cdr.markForCheck(); }
    });
  }

  loadKpi(): void {
    this.loadingKpi = true;
    this.cdr.markForCheck();
    this.svc.getKpi().subscribe({
      next: (k) => {
        this.kpi        = k;
        this.loadingKpi = false;
        this.cdr.markForCheck();
        if (k.has_data) {
          this.zone.runOutsideAngular(() => {
            const t = setTimeout(() => {
              this.initCharts();
              this.zone.run(() => this.cdr.markForCheck());
            }, 100);
            this.timers.push(t);
          });
        }
      },
      error: () => { this.loadingKpi = false; this.cdr.markForCheck(); }
    });
  }

  save(nc: SuiviNc): void {
    this.saving.add(nc.id);
    this.svc.evaluer(nc.id, {
      statut_impl:      nc.statut_impl,
      responsable_rssi: nc.responsable_rssi ?? undefined,
      echeance_rssi:    nc.echeance_rssi    ?? undefined,
      commentaire_rssi: nc.commentaire_rssi ?? undefined
    }).subscribe({
      next: (updated) => {
        const idx = this.liste.findIndex(x => x.id === nc.id);
        if (idx >= 0) this.liste[idx] = updated;
        this.saving.delete(nc.id);
        this.showSuccess('NC mise à jour');
        this.loadKpi();
        this.cdr.markForCheck();
      },
      error: () => { this.saving.delete(nc.id); this.cdr.markForCheck(); }
    });
  }

  // ── Charts ────────────────────────────────────────────────────
  private initCharts(): void {
    if (!this.kpi?.has_data) return;
    this.makeDoughnut();
    this.makeBarClause();
  }

  private makeDoughnut(): void {
    const el = this.getCanvas(this.ids.doughnut);
    if (!el || !this.kpi) return;
    this.destroyChart(this.ids.doughnut);
    const k = this.kpi;
    try {
      const c = new Chart(el, {
        type: 'doughnut',
        data: {
          labels: ['Fait', 'En cours', 'Non traité', 'Reporté', 'Accepté'],
          datasets: [{
            data: [k.nb_traites, k.nb_en_cours, k.nb_non_traites, k.nb_reportes, k.nb_acceptes],
            backgroundColor: ['#10b981','#f59e0b','#9ca3af','#6b7280','#3b82f6'],
            borderWidth: 2, borderColor: '#fff'
          }]
        },
        options: {
          animation: false, responsive: true, maintainAspectRatio: false,
          cutout: '65%',
          plugins: {
            legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 11 } } },
            tooltip: { callbacks: { label: ctx => ` ${ctx.label} : ${ctx.parsed}` } }
          }
        }
      });
      this.charts.set(this.ids.doughnut, c);
    } catch (e) { console.warn('NC doughnut:', e); }
  }

  private makeBarClause(): void {
    const el = this.getCanvas(this.ids.barClause);
    if (!el || !this.kpi?.par_clause?.length) return;
    this.destroyChart(this.ids.barClause);
    const clauses = this.kpi.par_clause;
    try {
      const c = new Chart(el, {
        type: 'bar',
        data: {
          labels: clauses.map(c => `Cl. ${c.clause_principale}`),
          datasets: [
            {
              label: 'Traités',
              data: clauses.map(c => c.nb_traites),
              backgroundColor: '#10b981',
              borderRadius: 4,
              borderSkipped: false
            },
            {
              label: 'Non traités',
              data: clauses.map(c => c.nb_non_traites),
              backgroundColor: '#ef4444',
              borderRadius: 4,
              borderSkipped: false
            }
          ]
        },
        options: {
          animation: false, responsive: true, maintainAspectRatio: false,
          plugins: {
            legend: { position: 'top', labels: { boxWidth: 10, font: { size: 11 } } },
            tooltip: { callbacks: {
              label: ctx => ` ${ctx.dataset.label} : ${ctx.parsed.y}`
            }}
          },
          scales: {
            y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.04)' }, ticks: { font: { size: 11 } } },
            x: { grid: { display: false }, ticks: { font: { size: 11 } } }
          }
        }
      });
      this.charts.set(this.ids.barClause, c);
    } catch (e) { console.warn('NC bar clause:', e); }
  }

  // ── Filtres ───────────────────────────────────────────────────
  get listeFiltree(): SuiviNc[] {
    return this.liste.filter(nc => {
      const ms = !this.filtreStatut   || nc.statut_impl  === this.filtreStatut;
      const mp = !this.filtrePriorite || nc.priorite     === this.filtrePriorite;
      const mc = !this.filtreClause   || nc.clause_code.startsWith(this.filtreClause);
      return ms && mp && mc;
    });
  }

  get clausesPrincipales(): string[] {
    return [...new Set(this.liste.map(nc => nc.clause_code.split('.')[0]))].sort();
  }

  // ── Helpers ───────────────────────────────────────────────────
  getStatutImplColor(s: string): string {
    return this.STATUTS_IMPL.find(x => x.value === s)?.color ?? '#9ca3af';
  }
  getStatutImplLabel(s: string): string {
    return this.STATUTS_IMPL.find(x => x.value === s)?.label ?? s;
  }
  getStatutAuditColor(s: string): string {
    return s === 'non_conforme' ? '#ef4444' : '#f59e0b';
  }
  getPrioriteColor(p: string): string {
    return ({ critique:'#7c3aed', haute:'#ef4444', normale:'#3b82f6', basse:'#9ca3af' } as Record<string,string>)[p] ?? '#6b7280';
  }
  isRetard(nc: SuiviNc): boolean {
    if (!nc.echeance_rssi) return false;
    return new Date(nc.echeance_rssi) < new Date() &&
           !['fait','accepte'].includes(nc.statut_impl);
  }
  isSaving(id: number): boolean { return this.saving.has(id); }

  private destroyChart(id: string): void {
    const c = this.charts.get(id);
    if (c) { try { c.destroy(); } catch (_) {} this.charts.delete(id); }
    const el = document.getElementById(id) as HTMLCanvasElement | null;
    if (el) { const ex = Chart.getChart(el); if (ex) { try { ex.destroy(); } catch (_) {} } }
  }

  private getCanvas(id: string): HTMLCanvasElement | null {
    const el = document.getElementById(id);
    return el instanceof HTMLCanvasElement ? el : null;
  }

  private showSuccess(msg: string): void {
    this.success = msg;
    const t = setTimeout(() => { this.success = ''; this.cdr.markForCheck(); }, 3000);
    this.timers.push(t);
  }
  switchTab(tab: 'liste' | 'kpi'): void {
  this.activeTab = tab;
  this.cdr.markForCheck();
}
}