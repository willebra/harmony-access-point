import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {SecurityService} from '../../security/security.service';

@Injectable()
export class DirtyGuard {

  constructor(private securityService: SecurityService) {
  };

  async canDeactivate(component: any, currentRoute: ActivatedRouteSnapshot, currentState: RouterStateSnapshot, nextState?: RouterStateSnapshot) {
    if (currentState.url == nextState.url) {
      return true;
    }
    return this.securityService.canAbandonUnsavedChanges(component);
  }

}
