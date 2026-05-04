import {
  Component,
  OnInit,
  OnDestroy,
  NgZone,
  ChangeDetectorRef,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule }  from '@angular/forms';
import { Router }       from '@angular/router';
import { KpiService } from '../../services/kpi.service';
import { AuthService }  from '../../services/auth.service';
import { KpiResponse }  from '../../models/kpi.models';

// ────────────────────────────────────────────────────────────
// IMPORT MINIMAL Chart.js — jamais registerables complet
// registerables complet inclut des plugins qui déclenchent des
// boucles d'animation infinies sur les canvas vides → crash.
// ────────────────────────────────────────────────────────────
import {
  Chart,
  RadarController,
  LineController,
  BarController,       // ← ajout
  RadialLinearScale,
  LinearScale,
  CategoryScale,
  PointElement,
  LineElement,
  BarElement,          // ← ajout

  Tooltip,
  Legend
} from 'chart.js';

Chart.register(
  RadarController,
  LineController,
  BarController,       // ← ajout
  RadialLinearScale,
  LinearScale,
  CategoryScale,
  PointElement,
  LineElement,
  BarElement,          // ← ajout
  Tooltip,
  Legend
);

// ────────────────────────────────────────────────────────────
// Désactiver les animations globalement — cause principale du
// blocage UI quand les données sont partielles ou nulles.
// ────────────────────────────────────────────────────────────
Chart.defaults.animation = false as any;
Chart.defaults.responsive = true;
Chart.defaults.maintainAspectRatio = false;

@Component({
  selector:        'app-kpi-dashboard',
  standalone:      true,
  imports:         [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl:     './kpi-dashboard.component.html',
  styleUrls:       ['./kpi-dashboard.component.scss']
})
export class KpiDashboardComponent implements OnInit, OnDestroy {

  currentUser:   any           = null;
  kpi:           KpiResponse | null = null;
  loading        = true;
  error          = '';
  periode        = 6;
  periodeOptions = [3, 6, 12];

  // IDs uniques par instance — évite les conflits si le
  // composant est instancié plusieurs fois ou après navigation.
  private readonly uid = Date.now().toString(36);
  readonly ids = {
    soaRadar: `kpi-soa-radar-${this.uid}`,
    soaLine:  `kpi-soa-line-${this.uid}`,
  pubBar:   `kpi-pub-bar-${this.uid}`,
    formBar:   `kpi-form-bar-${this.uid}`   // ← ajout

  };

  // Registre des charts créés — pour les détruire proprement
  private readonly charts = new Map<string, Chart>();

  // setTimeout handles — pour les annuler à destroy
  private timers: ReturnType<typeof setTimeout>[] = [];

  constructor(
    private svc:    KpiService,
    private auth:   AuthService,
    private router: Router,
    private zone:   NgZone,
    private cdr:    ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.loadKpi();
  }

  ngOnDestroy(): void {
    // Annuler tous les timers en attente
    this.timers.forEach(t => clearTimeout(t));
    // Détruire tous les charts — évite les memory leaks
    // et les erreurs "canvas already in use"
    this.destroyAll();
  }

  // ── Chargement ────────────────────────────────────────────
  loadKpi(): void {
    this.loading = true;
    this.destroyAll();
    this.cdr.markForCheck();

    this.svc.getDashboard(this.periode).subscribe({
      next: (data) => {
        this.kpi     = data;
        this.loading = false;
        this.error   = '';
        this.cdr.markForCheck();

        // Exécuter hors de la zone Angular pour ne pas
        // déclencher de cycle de détection pendant le rendu
        // des charts (cause d'un autre type de blocage).
        this.zone.runOutsideAngular(() => {
          // Double setTimeout : 1er pour laisser Angular
          // finir son rendu, 2e pour que le DOM soit peint.
          const t1 = setTimeout(() => {
            const t2 = setTimeout(() => {
              this.initCharts();
              // Revenir dans la zone pour la détection finale
              this.zone.run(() => this.cdr.markForCheck());
            }, 80);
            this.timers.push(t2);
          }, 0);
          this.timers.push(t1);
        });
      },
      error: () => {
        this.kpi     = null;
        this.loading = false;
        this.error   = 'Erreur de connexion au serveur KPI.';
        this.cdr.markForCheck();

        const t = setTimeout(() => {
          this.error = '';
          this.cdr.markForCheck();
        }, 6000);
        this.timers.push(t);
      }
    });
  }

  onPeriodeChange(): void {
    this.loadKpi();
  }

  // ── Init charts ───────────────────────────────────────────
  private initCharts(): void {
    if (!this.kpi?.has_data) return;

    if (this.kpi.soa?.has_data && this.kpi.soa.par_annexe?.length) {
      this.makeRadar(
        this.ids.soaRadar,
        this.kpi.soa.par_annexe.map(a => a.annexe),
        this.kpi.soa.par_annexe.map(a => this.safe(a.taux_annexe)),
        '#3b82f6'
      );
    }

    if (this.kpi.soa?.evolution?.length) {
      this.makeLine(
        this.ids.soaLine,
        this.kpi.soa.evolution.map(p => p.date.slice(0, 10)),
        [{ label: 'Conformité SOA (%)', data: this.kpi.soa.evolution.map(p => this.safe(p.taux_soa)), color: '#3b82f6' }]
      );
    }

 // ── Publication — une seule ligne journalière ────────────────────────
// ── Publication — bar chart par publication ──────────────────────────
if (this.kpi.publications?.has_data && this.kpi.publications.par_publication?.length) {
  this.makeBar(
    this.ids.pubBar,
    this.kpi.publications.par_publication
  );
}
// ── Formation — bar chart par session ────────────────────────────────
if (this.kpi.formation?.has_data && this.kpi.formation.par_session?.length) {
  this.makeFormationBar(
    this.ids.formBar,
    this.kpi.formation.par_session
  );
}
  }
  private makeFormationBar(
  id: string,
  sessions: { titre: string; statut: string; taux_participation: number;
              nb_inscrits: number; nb_presents: number;
              max_participants: number | null }[]
): void {
  if (!sessions.length) return;

  const el = this.getCanvas(id);
  if (!el) return;

  this.destroyChart(id);

  const colorMap: Record<string, string> = {
    planifie: '#3b82f6',
    en_cours: '#f59e0b',
    termine:  '#10b981',
    annule:   '#ef4444'
  };

  const colors = sessions.map(s => colorMap[s.statut] ?? '#6b7280');
  const labels = sessions.map(s =>
    s.titre.length > 22 ? s.titre.slice(0, 20) + '…' : s.titre
  );

  try {
    const chart = new Chart(el, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label:           'Taux de participation (%)',
          data:            sessions.map(s => this.safe(s.taux_participation)),
          backgroundColor: colors,
          borderRadius:    6,
          borderSkipped:   false
        }]
      },
      options: {
        animation:           false,
        responsive:          true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              title: (items) => sessions[items[0].dataIndex].titre,
              label: (ctx) => {
                const s = sessions[ctx.dataIndex];
                const denom = s.max_participants ?? s.nb_inscrits;
                return [
                  ` Participation : ${(ctx.parsed.y as number).toFixed(1)}%`,
                  ` Présents : ${s.nb_presents} / ${denom}`
                ];
              }
            }
          }
        },
        scales: {
          y: {
            min:  0,
            max:  100,
            grid: { color: 'rgba(0,0,0,0.04)' },
            ticks: {
              font:     { size: 11 },
              stepSize: 25,
              callback: (v) => v + '%'
            }
          },
          x: {
            grid:  { display: false },
            ticks: {
              font:        { size: 10 },
              maxRotation: 35,
              minRotation: 35
            }
          }
        }
      }
    });
    this.charts.set(id, chart);
  } catch (e) {
    console.warn(`Chart bar formation [${id}] init error:`, e);
  }
}

// Helper label statut (utilisé dans le template)
getStatutFormationColor(statut: string): string {
  return ({
    planifie: '#3b82f6',
    en_cours: '#f59e0b',
    termine:  '#10b981',
    annule:   '#ef4444'
  } as Record<string, string>)[statut] ?? '#6b7280';
}

getStatutFormationLabel(statut: string): string {
  return ({
    planifie: 'Planifiée',
    en_cours: 'En cours',
    termine:  'Terminée',
    annule:   'Annulée'
  } as Record<string, string>)[statut] ?? statut;
}

  // ── Factory radar ─────────────────────────────────────────
  private makeRadar(
    id: string,
    labels: string[],
    data: number[],
    color: string
  ): void {
    // Ne rien faire si toutes les valeurs sont 0 — évite
    // un canvas "vide" qui consomme du CPU pour rien.
    if (data.every(v => v === 0)) return;

    const el = this.getCanvas(id);
    if (!el) return;

    this.destroyChart(id);

    try {
      const chart = new Chart(el, {
        type: 'radar',
        data: {
          labels,
          datasets: [{
            label:                'Taux (%)',
            data,
            backgroundColor:      `${color}22`,
            borderColor:          color,
            pointBackgroundColor: color,
            pointRadius:          5,
            pointHoverRadius:     7,
            borderWidth:          2
          }]
        },
        options: {
          animation:           false,
          responsive:          true,
          maintainAspectRatio: false,
          scales: {
            r: {
              min: 0, max: 100,
              ticks: {
                stepSize: 25,
                callback: (v) => v + '%',
                font: { size: 10 },
                color: '#9ca3af'
              },
              pointLabels: {
                font:  { size: 12, weight: 600 },
                color: '#374151'
              },
              grid: { color: 'rgba(0,0,0,0.07)' }
            }
          },
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                label: ctx => ` ${(ctx.parsed.r as number).toFixed(1)}%`
              }
            }
          }
        }
      });
      this.charts.set(id, chart);
    } catch (e) {
      console.warn(`Chart radar [${id}] init error:`, e);
    }
  }

  // ── Factory line ──────────────────────────────────────────
  private makeLine(
    id: string,
    labels: string[],
    series: { label: string; data: number[]; color: string }[]
  ): void {
    if (!labels.length) return;

    const el = this.getCanvas(id);
    if (!el) return;

    this.destroyChart(id);

    try {
      const chart = new Chart(el, {
        type: 'line',
        data: {
          labels,
          datasets: series.map(s => ({
            label:           s.label,
            data:            s.data,
            borderColor:     s.color,
            backgroundColor: `${s.color}14`,
            tension:         0.35,
            fill:            true,
            pointRadius:     4,
            pointHoverRadius:6,
            borderWidth:     2
          }))
        },
        options: {
          animation:           false,
          responsive:          true,
          maintainAspectRatio: false,
          interaction:         { mode: 'index', intersect: false },
          plugins: {
            legend: {
              position: 'top',
              labels:   { boxWidth: 10, font: { size: 11 } }
            },
            tooltip: {
              callbacks: {
             label: (ctx) => {
  const v = ctx.parsed.y;

  if (typeof v !== 'number' || !isFinite(v)) {
    return '';
  }

  return ` ${ctx.dataset.label}: ${v.toFixed(1)}`;
}
              }
            }
          },
          scales: {
           y: {
  min: 0,
  max: 100,
  grid: { color: 'rgba(0,0,0,0.04)' },
  ticks: {
    font: { size: 11 },
    stepSize: 25,
    callback: (v) => v + '%'
  }
},
            x: {
              grid:  { display: false },
              ticks: { font: { size: 11 }, maxTicksLimit: 8 }
            }
          }
        }
      });
      this.charts.set(id, chart);
    } catch (e) {
      console.warn(`Chart line [${id}] init error:`, e);
    }
  }
private makeBar(
  id: string,
  pubs: { titre: string; taux_lecture: number; priorite: string }[]
): void {
  if (!pubs.length) return;

  const el = this.getCanvas(id);
  if (!el) return;

  this.destroyChart(id);

  // Couleur selon priorité
  const colors = pubs.map(p => {
    switch (p.priorite) {
      case 'urgente': return '#ef4444';
      case 'haute':   return '#f59e0b';
      case 'normale': return '#3b82f6';
      case 'basse':   return '#9ca3af';
      default:        return '#6b7280';
    }
  });

  // Tronquer les titres longs pour l'axe X
  const labels = pubs.map(p =>
    p.titre.length > 22 ? p.titre.slice(0, 20) + '…' : p.titre
  );

  try {
    const chart = new Chart(el, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label:           'Taux de lecture (%)',
          data:            pubs.map(p => this.safe(p.taux_lecture)),
          backgroundColor: colors,
          borderRadius:    6,
          borderSkipped:   false
        }]
      },
      options: {
        animation:           false,
        responsive:          true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              title: (items) => pubs[items[0].dataIndex].titre,
              label: (ctx)  => ` ${(ctx.parsed.y as number).toFixed(1)}%`
            }
          }
        },
        scales: {
          y: {
            min:  0,
            max:  100,
            grid: { color: 'rgba(0,0,0,0.04)' },
            ticks: {
              font:      { size: 11 },
              stepSize:  25,
              callback:  (v) => v + '%'
            }
          },
          x: {
            grid:  { display: false },
            ticks: {
              font:          { size: 10 },
              maxRotation:   35,
              minRotation:   35
            }
          }
        }
      }
    });
    this.charts.set(id, chart);
  } catch (e) {
    console.warn(`Chart bar [${id}] init error:`, e);
  }
}
  // ── Destruction ───────────────────────────────────────────
  private destroyChart(id: string): void {
    // 1. Détruire via notre registre
    const c = this.charts.get(id);
    if (c) {
      try { c.destroy(); } catch (_) {}
      this.charts.delete(id);
    }
    // 2. Sécurité : détruire aussi via le registre Chart.js
    //    (au cas où le canvas serait réutilisé)
    const el = document.getElementById(id) as HTMLCanvasElement | null;
    if (el) {
      const existing = Chart.getChart(el);
      if (existing) {
        try { existing.destroy(); } catch (_) {}
      }
    }
  }

  private destroyAll(): void {
    Object.values(this.ids).forEach(id => this.destroyChart(id));
  }

  // ── Helpers DOM ───────────────────────────────────────────
  private getCanvas(id: string): HTMLCanvasElement | null {
    const el = document.getElementById(id);
    if (!el) {
      console.warn(`[KPI] Canvas #${id} non trouvé`);
      return null;
    }
    if (!(el instanceof HTMLCanvasElement)) {
      console.warn(`[KPI] #${id} n'est pas un canvas`);
      return null;
    }
    return el;
  }

  // ── Helpers affichage ─────────────────────────────────────
  private safe(v: unknown): number {
    const n = Number(v);
    return isFinite(n) ? Math.max(0, Math.min(100, n)) : 0;
  }

  labelType(t: string): string {
    return ({
      information: 'Info',
      alerte:      'Alerte',
      procedure:   'Procédure',
      politique:   'Politique',
      note:        'Note'
    } as Record<string, string>)[t] ?? t;
  }

  getColor(v: number): string {
    const n = Number(v) || 0;
    return n >= 80 ? '#10b981' : n >= 50 ? '#f59e0b' : '#ef4444';
  }

  getLabel(v: number): string {
    const n = Number(v) || 0;
    return n >= 80 ? 'Satisfaisant' : n >= 50 ? 'À améliorer' : 'Insuffisant';
  }

  getPrioriteColor(p: string): string {
    return ({ urgente: '#ef4444', haute: '#f59e0b', normale: '#3b82f6', basse: '#9ca3af' } as Record<string, string>)[p] ?? '#6b7280';
  }

  retour(): void {
    const map: Record<string, string> = {
      rssi:           '/dashboard/rssi',
      direction:      '/dashboard/direction',
      auditeur:       '/dashboard/rssi',
      admin_organism: '/dashboard/admin',
      super_admin:    '/dashboard/super-admin'
    };
    this.router.navigate([map[this.currentUser?.role] ?? '/']);
  }
}