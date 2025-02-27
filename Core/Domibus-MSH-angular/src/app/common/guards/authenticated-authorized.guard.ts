﻿import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree} from '@angular/router';
import {SecurityService} from '../../security/security.service';
import {DomibusInfoService} from '../appinfo/domibusinfo.service';
import {SessionState} from '../../security/SessionState';

/**
 * It will handle for each route where is defined:
 * - authentication
 * - authorization - only if the route has data: checkRoles initialized
 */
@Injectable()
export class AuthenticatedAuthorizedGuard {

  constructor(private router: Router, private securityService: SecurityService,
              private domibusInfoService: DomibusInfoService) {
  }

  async canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
    const isAuthenticated = await this.securityService.isAuthenticated();
    if (!isAuthenticated) {
      this.handleNotAuthenticated();
      return this.getNotAuthenticatedRoute(state);
    }

    // make sure the app is properly initialized before checking anything else
    await this.securityService.isAppInitialized();

    const isAuthorized = await this.isAuthorized(route);
    if (!isAuthorized) {
      return this.getNotAuthorizedRoute();
    }

    return true;
  }

  private async isAuthorized(route: ActivatedRouteSnapshot): Promise<boolean> {
    let allowedRoles;
    const routeData = route.data;
    if (!!routeData.checkRolesFn) {
      allowedRoles = await routeData.checkRolesFn.call();
    } else {
      allowedRoles = routeData.checkRoles
    }
    return this.securityService.isCurrentUserInRole(allowedRoles);
  }

  private getNotAuthorizedRoute(): UrlTree {
    // needs to be a route without authorization guard, otherwise it will loop forever
    return this.router.parseUrl('/notAuthorized');
  }

  private handleNotAuthenticated() {
    this.securityService.clearAppData(SessionState.EXPIRED_INACTIVITY_OR_ERROR);
  }

  private async getNotAuthenticatedRoute(state: RouterStateSnapshot): Promise<UrlTree> {
    let isExtAuthProvider = await this.domibusInfoService.isExtAuthProviderEnabled();
    // not logged in so redirect to login page with the return url
    if (!isExtAuthProvider) {
      return this.router.createUrlTree(['/login'], {queryParams: {returnUrl: state.url}});
    }
    // EU Login redirect to logout
    return this.router.createUrlTree(['/logout']);
  }
}
