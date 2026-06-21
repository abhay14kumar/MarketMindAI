export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Source {
  id: string;
  code: string;
  name: string;
  organization: string;
  sourceType: string;
  status: string;
  authenticationType: string;
  refreshFrequency: string;
  baseUrl: string;
  robotsUrl: string | null;
  samplePdfUrl: string | null;
  capabilities: string[];
  enabled: boolean;
  priority: number;
  reliabilityScore: number;
}

export interface SourceHealth {
  id: string;
  sourceId: string;
  status: string;
  available: boolean;
  latencyMs: number;
  message: string;
  checkedAt: string;
  lastHttpStatus: number | null;
  lastLatencyMs: number | null;
  robotsTxtAvailable: boolean | null;
  robotsTxtStatus: number | null;
  pdfCapabilityStatus: string | null;
  lastValidatedAt: string | null;
}

export interface SourceValidation {
  sourceId: string;
  sourceName: string;
  reachable: boolean;
  httpStatus: number | null;
  latencyMs: number;
  robotsTxtAvailable: boolean;
  robotsTxtStatus: number | null;
  pdfCapabilityStatus: string;
  validationStatus: string;
  message: string;
  validatedAt: string;
}

export interface DocumentRecord {
  id: string;
  companyId: string | null;
  sourceCode: string | null;
  sourceName: string | null;
  documentType: string;
  title: string;
  sourceUrl: string;
  publicationDate: string | null;
  reportingPeriod: string | null;
  fiscalYear: number | null;
  quarter: string | null;
  status: string;
  createdAt: string;
}

export interface DownloadJob {
  id: string;
  documentId: string | null;
  sourceId: string | null;
  requestedUrl: string;
  status: string;
  attemptCount: number;
  maxAttempts: number;
  submittedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  errorCode: string | null;
  errorMessage: string | null;
}

export interface SchedulerJob {
  id: string;
  name: string;
  description: string | null;
  schedulerType: string;
  status: string;
  cronExpression: string;
  timeZone: string;
  nextRunAt: string | null;
  lastRunAt: string | null;
}

export interface QueryPayload<T> {
  data: T;
  fallback: boolean;
  fallbackReason?: string;
}

export interface PortfolioSummary {
  totalInvestedValue: number;
  totalPresentValue: number;
  totalUnrealizedPnl: number;
  totalUnrealizedPnlPercentage: number;
  totalCurrentValue: number;
  totalPnl: number;
  totalPnlPercentage: number;
  dayPnl: number;
  dayPnlPercentage: number;
  totalHoldings: number;
  lastImportedAt: string | null;
  latestPriceAt: string | null;
}

export interface PortfolioHolding {
  id: string;
  symbol: string;
  isin: string | null;
  companyName: string | null;
  sector: string | null;
  instrumentType: string;
  quantity: number;
  averageCost: number;
  lastPrice: number | null;
  previousClose: number | null;
  investedValue: number;
  presentValue: number;
  unrealizedPnl: number;
  unrealizedPnlPercentage: number;
  asOf: string;
  currentPrice: number | null;
  currentValue: number | null;
  totalPnl: number | null;
  totalPnlPercentage: number | null;
  dayPnl: number | null;
  dayPnlPercentage: number | null;
  priceSource: string | null;
  priceCapturedAt: string | null;
}

export interface ManualPriceRequest {
  symbol: string;
  exchange: 'NSE' | 'BSE' | 'UNKNOWN';
  lastPrice: number;
  previousClose: number;
  source: 'MANUAL' | 'MOCK';
}

export interface PriceSnapshot {
  id: string;
  symbol: string;
  exchange: string;
  lastPrice: number;
  previousClose: number;
  source: string;
  capturedAt: string;
}

export interface PriceFeedJob {
  id: string;
  source: string;
  provider: string | null;
  status: string;
  requestedInstruments: number;
  updatedInstruments: number;
  failedInstruments: number;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface PriceProviderStatus {
  configuredProvider: string;
  scheduledRefreshEnabled: boolean;
  refreshIntervalSeconds: number;
  lastRefreshStatus: string | null;
  requestedSymbols: number;
  successfulSymbols: number;
  failedSymbols: number;
  errorSummary: string | null;
  lastRefreshAt: string | null;
}

export interface PortfolioAllocation {
  category: string;
  presentValue: number;
  percentage: number;
}

export interface PortfolioRowError {
  rowNumber: number;
  message: string;
}

export interface PortfolioImportJob {
  id: string;
  brokerType: string;
  originalFileName: string;
  status: string;
  totalRows: number;
  importedRows: number;
  rejectedRows: number;
  rowErrors: PortfolioRowError[];
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface PortfolioImportResponse {
  importJob: PortfolioImportJob;
  summary: PortfolioSummary;
}

export interface AiCitation {
  documentId: string;
  chunkId: string;
  chunkIndex: number;
  snippet: string;
}

export interface AiAnswer {
  id: string;
  question: string;
  answer: string;
  documentId: string | null;
  status: string;
  confidenceScore: number;
  citations: AiCitation[];
  createdAt: string;
}

export interface AiAskRequest {
  question: string;
  documentId?: string;
  topK: number;
}

export interface EmbeddingJob {
  id: string;
  documentId: string;
  documentVersionId: string;
  status: string;
  totalChunks: number;
  embeddedChunks: number;
  failedChunks: number;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface DocumentChunk {
  id: string;
  documentId: string;
  documentVersionId: string;
  chunkIndex: number;
  chunkText: string;
  tokenCount: number;
  characterCount: number;
  qdrantCollection: string | null;
  qdrantPointId: string | null;
  createdAt: string;
}
