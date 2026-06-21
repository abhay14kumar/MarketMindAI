import type {
  DocumentRecord,
  DownloadJob,
  SchedulerJob,
  Source,
  SourceHealth,
} from './types';

export const fallbackSources: Source[] = [
  {
    id: 'fallback-nse',
    code: 'NSE',
    name: 'National Stock Exchange of India',
    organization: 'National Stock Exchange of India Limited',
    sourceType: 'EXCHANGE',
    status: 'ACTIVE',
    authenticationType: 'SESSION',
    refreshFrequency: 'MINUTELY',
    baseUrl: 'https://www.nseindia.com',
    robotsUrl: 'https://www.nseindia.com/robots.txt',
    samplePdfUrl: null,
    capabilities: ['MARKET_DATA', 'CORPORATE_FILINGS', 'FINANCIAL_RESULTS'],
    enabled: true,
    priority: 10,
    reliabilityScore: 0.98,
  },
  {
    id: 'fallback-w3c',
    code: 'W3C_TEST',
    name: 'W3C Test Source',
    organization: 'World Wide Web Consortium',
    sourceType: 'TEST_SOURCE',
    status: 'ACTIVE',
    authenticationType: 'NONE',
    refreshFrequency: 'ON_DEMAND',
    baseUrl: 'https://www.w3.org',
    robotsUrl: 'https://www.w3.org/robots.txt',
    samplePdfUrl: 'https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf',
    capabilities: ['HTTP_REACHABILITY', 'ROBOTS_TXT', 'PDF_DOWNLOAD'],
    enabled: true,
    priority: 100,
    reliabilityScore: 0.99,
  },
];

export const fallbackSourceHealth: SourceHealth[] = fallbackSources.map((source, index) => ({
  id: `fallback-health-${index}`,
  sourceId: source.id,
  status: index === 0 ? 'DEGRADED' : 'ACTIVE',
  available: index !== 0,
  latencyMs: index === 0 ? 720 : 145,
  message: 'Fallback demonstration health record.',
  checkedAt: '2026-06-20T12:00:00Z',
  lastHttpStatus: index === 0 ? null : 200,
  lastLatencyMs: index === 0 ? 720 : 145,
  robotsTxtAvailable: index !== 0,
  robotsTxtStatus: index === 0 ? null : 200,
  pdfCapabilityStatus: index === 0 ? 'UNKNOWN' : 'SUPPORTED',
  lastValidatedAt: '2026-06-20T12:00:00Z',
}));

export const fallbackDocuments: DocumentRecord[] = [
  {
    id: 'fallback-document-1',
    companyId: null,
    sourceCode: 'NSE',
    sourceName: 'National Stock Exchange of India',
    documentType: 'ANNUAL_REPORT',
    title: 'Example Annual Report',
    sourceUrl: 'https://example.com/annual-report.pdf',
    publicationDate: '2026-05-30',
    reportingPeriod: 'FY2025-26',
    fiscalYear: 2026,
    quarter: null,
    status: 'INDEXED',
    createdAt: '2026-06-20T12:00:00Z',
  },
];

export const fallbackDownloadJobs: DownloadJob[] = [
  {
    id: 'fallback-download-job-1',
    documentId: 'fallback-document-1',
    sourceId: 'fallback-nse',
    requestedUrl: 'https://example.com/annual-report.pdf',
    status: 'COMPLETED',
    attemptCount: 1,
    maxAttempts: 3,
    submittedAt: '2026-06-20T11:58:00Z',
    startedAt: '2026-06-20T11:58:02Z',
    completedAt: '2026-06-20T11:58:06Z',
    errorCode: null,
    errorMessage: null,
  },
];

export const fallbackSchedulerJobs: SchedulerJob[] = [
  {
    id: 'fallback-scheduler-1',
    name: 'Daily filing discovery',
    description: 'Demonstration scheduler configuration.',
    schedulerType: 'DOCUMENT_INGESTION',
    status: 'ACTIVE',
    cronExpression: '0 0 6 * * *',
    timeZone: 'Asia/Kolkata',
    nextRunAt: '2026-06-21T00:30:00Z',
    lastRunAt: '2026-06-20T00:30:00Z',
  },
];
