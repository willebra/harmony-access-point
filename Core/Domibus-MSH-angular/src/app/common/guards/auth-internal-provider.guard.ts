import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot} from '@angular/router';
import {DomibusInfoService} from "../appinfo/domibusinfo.service";

/**
 * It will redirect to home ('/') if the external provider = false
 */
@Injectable()
export class AuthInternalProviderGuard {

  constructor(private router: Router, private domibusInfoService: DomibusInfoService) {
  }

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const isUserFromExternalAuthProvider = await this.domibusInfoService.isExtAuthProviderEnabled();

    if (!isUserFromExternalAuthProvider) {
      this.router.navigate(['/']);
      return false;
    }
    return true;
  }
}
