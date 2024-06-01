import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, RouterStateSnapshot} from '@angular/router';
import {SecurityService} from '../../security/security.service';

@Injectable()
export class DefaultPasswordGuard {

  constructor(private securityService: SecurityService) {
  };

  async canActivate(next: ActivatedRouteSnapshot, state: RouterStateSnapshot) {

    await this.securityService.isAppInitialized();

    return !this.securityService.mustChangePassword();

  }

}
