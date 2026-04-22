import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/models';
import { filter } from 'rxjs/operators';

interface NavItem {
  label:    string;
  icon:     string;
  route:    string;
  clause?:  string;
  disabled?: boolean;
  roles:    string[];  // ← rôles autorisés
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  sidebarCollapsed = false;
  activeRoute      = '';
  showUserMenu     = false;
isRiskAnalysisEnabled = false;
  // Toutes les entrées de nav avec les rôles autorisés
  allNavItems: NavItem[] = [
    {
      label:  'Organismes',
      icon:   'building',
      route:  '/dashboard/organisms',
      clause: 'Administration',
      roles:  ['super_admin', 'admin_organism']
    },
    {
      label:  'Contexte',
      icon:   'context',
      route:  '/dashboard/clause4',
      clause: 'Clause 4',
      roles:  ['super_admin', 'rssi']
    },
 
    {
  label:  'Protection des données',
  icon:   'shield',
  route:  '/dpo',
  clause: 'DPO',
  roles:  ['dpo', 'super_admin']
},
{
  label: 'Fiche technique',
  icon:  'tool',
  route: '/dashboard/fiche-technique',
  roles: ['membre_equipe_technique', 'super_admin']
},
{
  label: 'Tableau de bord RSSI',
  icon:  'shield',
  route: '/dashboard/rssi',
  roles: ['rssi', 'super_admin']
},
{
  label: 'Direction — Clause 5',
  icon:  'briefcase',
  route: '/dashboard/direction',
  roles: ['direction', 'comite_securite', 'super_admin']
},
{
  label: 'Clause 6 — Planification',
  icon:  'target',
  route: '/dashboard/clause6',
  roles: ['rssi', 'super_admin']
},
{
  label: 'Clause 6 — Planification',
  icon: 'target',
  route: '/dashboard/direction/clause6',
  roles: ['direction', 'comite_securite', 'super_admin']
},
{
  label: 'Analyse de risque',
  icon: 'risk',
  route: '/dashboard/ebios',
  roles: ['rssi', 'super_admin'],
  disabled: true // sera dynamique
},
{
  label: 'Registre des traitements',
  icon: 'database',
  route: '/dashboard/dpo/registre',  // ✅ CORRECT
  roles: ['dpo', 'super_admin']
},
{
  label:  'Clause 7 — Support',
  icon:   'support',
  route:  '/dashboard/clause7',
  clause: 'Clause 7',
  roles:  [
    'rssi',
    'admin_organism',
    'super_admin',
    'direction',
    'employe',
    'pilote_processus',
    'dpo'
  ]
}
  ];

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.currentUser = this.auth.getCurrentUser();
    this.checkRiskAccess(); 
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => { this.activeRoute = e.url; });
    this.activeRoute = this.router.url;
  }
checkRiskAccess() {


  this.isRiskAnalysisEnabled = true; // ⚠️ test temporaire
}
  // Items visibles selon le rôle
  get navItems(): NavItem[] {
  const role = this.currentUser?.role || '';

  return this.allNavItems
    .map(item => {
      if (item.label === 'Analyse de risque') {
        return {
          ...item,
          disabled: !this.isRiskAnalysisEnabled
        };
      }
      return item;
    })
    .filter(item => item.roles.includes(role));
}

  get userInitials(): string {
    if (!this.currentUser) return 'AD';
    return (this.currentUser.prenom[0] + this.currentUser.nom[0]).toUpperCase();
  }

  get roleLabel(): string {
    const labels: Record<string, string> = {
      super_admin:    'Super Administrateur',
      admin_organism: 'Admin Organisme',
      rssi:           'RSSI'
    };
    return labels[this.currentUser?.role || ''] || this.currentUser?.role || '';
  }

  navigate(item: NavItem): void {
    if (item.disabled || !item.route) return;
    this.router.navigate([item.route]);
  }

  isActive(route: string): boolean { return route ? this.activeRoute.startsWith(route) : false; }
  toggleSidebar():  void { this.sidebarCollapsed = !this.sidebarCollapsed; }
  toggleUserMenu(): void { this.showUserMenu = !this.showUserMenu; }
  logout():         void { this.auth.logout(); }
}