import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () =>
      import('./components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'change-password',
    loadComponent: () =>
      import('./components/change-password/change-password.component')
        .then(m => m.ChangePasswordComponent),
    canActivate: [AuthGuard]
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'organisms', pathMatch: 'full' },
      {
        path: 'organisms',
        loadComponent: () =>
          import('./components/organisms/organisms.component').then(m => m.OrganismsComponent)
      },
      {
        path: 'organisms/:id/actors',
        loadComponent: () =>
          import('./components/actors/actors.component').then(m => m.ActorsComponent)
      },
   
      {
        path: 'processus',   
        loadComponent: () =>
          import('./components/fiche-processus/fiche-processus.component')
            .then(m => m.FicheProcessusComponent)
      },
        {
      path: 'dpo',
      loadComponent: () =>
        import('./components/dpo/dpo.component').then(m => m.DpoComponent)
    },
    {
  path: 'fiche-technique',
  loadComponent: () =>
    import('./components/fiche-technique/fiche-technique.component')
      .then(m => m.FicheTechniqueComponent)
},
{
  path: 'rssi',
  loadComponent: () =>
    import('./components/rssi/rssi.component').then(m => m.RssiComponent)
},
{
  path: 'direction',
  loadComponent: () =>
    import('./components/direction/direction.component')
      .then(m => m.DirectionComponent)
},
{
path: 'direction/clause6',
loadComponent: () =>
  import('./components/direction-clause6/direction-clause6.component')
    .then(m => m.DirectionClause6Component)
},
 {
  path: 'clause6',
  loadComponent: () =>
    import('./components/clause6-rssi/clause6-rssi.component')
      .then(m => m.Clause6RssiComponent)
},
{
  path: 'ebios',
  loadComponent: () =>
    import('./components/ebios/ebios.component').then(m => m.EbiosComponent)
},
 {
  path: 'dpo/registre',
  loadComponent: () =>
    import('./components/registre/registre.component')
      .then(m => m.RegistreComponent),
},
  {
    path: 'clause7',
    loadComponent: () =>
      import('./components/clause7/clause7.component')
        .then(m => m.Clause7Component)
  }
    ]
  },
 
  { path: '**', redirectTo: '/login' }
];