import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ReportService } from './report.service';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('file POSTs a report', () => {
    service.file({ subjectType: 'MANAGER', subjectId: 10, travelId: null, reason: 'x' }).subscribe();
    const req = httpMock.expectOne({ method: 'POST', url: '/api/travels/reports' });
    expect(req.request.body.subjectId).toBe(10);
    req.flush({});
  });

  it('all GETs every report', () => {
    service.all().subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/reports' }).flush([]);
  });

  it('mine GETs own reports', () => {
    service.mine().subscribe();
    httpMock.expectOne({ method: 'GET', url: '/api/travels/reports/mine' }).flush([]);
  });

  it('review PATCHes a report', () => {
    service.review(3).subscribe();
    httpMock.expectOne({ method: 'PATCH', url: '/api/travels/reports/3/review' }).flush({});
  });
});
