import {Injectable} from '@angular/core';

@Injectable()
export class DateService {

  todayEndDay() {
    const today = new Date();
    today.setHours(23, 59, 59, 999);
    return today;
  }

  setEndDay(date: Date) {
    if (!date.getHours() && !date.getMinutes() && !date.getSeconds()) {
      date.setHours(23, 59, 59, 999);
    }
  }

  private getFormattedDate(date: Date): string {
    return date.getFullYear().toString() + '-' +
      ('0' + (date.getMonth() + 1)).slice(-2).toString() + '-' +
      ('0' + date.getDate()).slice(-2).toString();
  }

  private getFormattedTime(date: Date): string {
    return ('0' + date.getHours()).slice(-2).toString() +
      ('0' + date.getMinutes()).slice(-2).toString() +
      ('0' + date.getSeconds()).slice(-2).toString();
  }

  format(date: Date): string {
    return this.getFormattedDate(date) + '_' + this.getFormattedTime(date);
  }
}
