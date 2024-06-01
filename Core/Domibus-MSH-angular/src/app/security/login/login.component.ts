import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {SecurityService} from '../security.service';
import {AlertService} from '../../common/alert/alert.service';
import {Server} from '../Server';

@Component({
  templateUrl: 'login.component.html',
  styleUrls: ['./login.component.css']
})

export class LoginComponent implements OnInit {
  model: any = {};
  returnUrl: string;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private securityService: SecurityService,
              private alertService: AlertService) {
  }

  ngOnInit() {
    // get return url from route parameters or default to '/'
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
  }

  async login() {
    try {
      await this.securityService.login(this.model.username, this.model.password, this.returnUrl);
    } catch (ex) {
      this.onLoginError(ex);
    }
  }

  onLoginError(error) {
    let message;
    switch (error.status) {
      case Server.HTTP_UNAUTHORIZED:
      case Server.HTTP_FORBIDDEN:
        message = 'The username/password combination you provided is not valid. Please try again or contact your administrator.';
        break;
      case Server.HTTP_GATEWAY_TIMEOUT:
      case Server.HTTP_NOTFOUND:
        message = 'Unable to login. Harmony Access Point is not running.';
        break;
      default:
        this.alertService.exception('Error authenticating:', error);
        return;
    }
    this.alertService.error(message);
  }

}
