import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { MAT_DIALOG_DEFAULT_OPTIONS } from '@angular/material/dialog';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './core/auth/auth.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAnimationsAsync(),
    {
      // Every dialog.open() call sets a fixed `width` sized for desktop;
      // this caps it so none of them overflow a mobile viewport, without
      // having to repeat maxWidth at every call site.
      provide: MAT_DIALOG_DEFAULT_OPTIONS,
      useValue: { maxWidth: '95vw' },
    },
  ],
};
