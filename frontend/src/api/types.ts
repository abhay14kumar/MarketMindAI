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

export interface SourceIntelligenceCatalogItem {
  id: string;
  code: string;
  name: string;
  organization: string;
  sourceType: string;
  status: string;
  baseUrl: string;
  official: boolean;
  priority: number;
  reliabilityScore: number;
  trustScore: number;
  freshnessScore: number;
  trustTier: string;
  connectorType: string;
  capabilities: string[];
  supportedFormats: string[];
  supportedDocumentTypes: string[];
  healthy: boolean | null;
  latencyMs: number | null;
  httpStatus: number | null;
  lastValidatedAt: string | null;
  lastCrawlAt: string | null;
  nextCrawlAt: string | null;
  schedulerState: string;
  totalCrawls: number;
  successfulCrawls: number;
  failedCrawls: number;
  documentsDiscovered: number;
}

export interface SourceIntelligenceMetrics {
  totalSources: number;
  officialSources: number;
  healthySources: number;
  degradedSources: number;
  enabledConnectors: number;
  discoveryJobs: number;
  pipelineJobs: number;
  documentsDiscovered: number;
  averageTrustScore: number;
  averageReliability: number;
  coveragePercent: number;
}

export interface SourceCoverageRow {
  companySymbol: string;
  documentType: string;
  discoveredCount: number;
  newCount: number;
  ingestedCount: number;
  aiReadyCount: number;
  coverageStatus: string;
}

export interface SourceActivity {
  id: string;
  sourceId: string | null;
  activityType: string;
  severity: string;
  title: string;
  message: string;
  relatedEntityType: string | null;
  relatedEntityId: string | null;
  occurredAt: string;
  createdAt: string;
}

export interface SourceConnectorDescriptor {
  connectorType: string;
  trustTier: string;
  supportedFormats: string[];
  supportedDocumentTypes: string[];
}

export interface SourceRefreshResult {
  sourceId: string;
  discoveryJobId: string;
  status: string;
  connectorType: string;
  documentsDiscovered: number;
  message: string;
  recommendation: string | null;
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
  configuration?: Record<string, string>;
  executionMode?: 'MOCK' | 'MANUAL' | 'REAL';
  implementationStatus?: 'SEEDED' | 'WIRED' | 'PARTIAL' | 'NOT_IMPLEMENTED';
  nextRunSeeded?: boolean;
  lastRunSeeded?: boolean;
  nextRunAt: string | null;
  lastRunAt: string | null;
  lastRunStatus?: string | null;
  lastRunMessage?: string | null;
  lastRunDurationMs?: number | null;
  lastRunStartedAt?: string | null;
  lastRunCompletedAt?: string | null;
  createdDocumentsCount?: number;
  pipelineJobsCreatedCount?: number;
}

export interface SchedulerRun {
  id: string;
  schedulerJobId: string;
  status: string;
  triggerType: string;
  queuedAt: string;
  startedAt: string | null;
  completedAt: string | null;
  durationMs: number;
  processedItems: number;
  resultSummary: string | null;
  errorMessage: string | null;
  discoveredDocumentsCount: number;
  pipelineJobsCreatedCount: number;
  correlationId: string;
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

export interface PipelineStep {
  id: string;
  stepName: 'DOWNLOAD' | 'TEXT_EXTRACTION' | 'CHUNKING' | 'EMBEDDING' | 'AI_READY';
  status: 'STARTED' | 'COMPLETED' | 'FAILED' | 'SKIPPED' | 'PARTIAL';
  startedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
  retryCount: number;
}

export interface PipelineRun {
  id: string;
  documentId: string;
  documentTitle: string;
  status: 'STARTED' | 'COMPLETED' | 'FAILED' | 'SKIPPED' | 'PARTIAL';
  currentStep: PipelineStep['stepName'];
  startedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
  createdAt: string;
  steps: PipelineStep[];
}

export interface DiscoveryRunRequest {
  sourceType: 'TEST_SOURCE' | 'NSE' | 'BSE' | 'SEBI' | 'RBI' | 'COMPANY_IR';
  sourceUrl?: string;
  companySymbol?: string;
  maxDocuments: number;
}

export interface DiscoveryJob {
  discoveryJobId: string;
  sourceType: string;
  sourceUrl: string | null;
  status: string;
  totalDiscovered: number;
  newDocuments: number;
  existingDocuments: number;
  ignoredDocuments: number;
  failedSources: number;
  message: string | null;
  recommendation: string | null;
  crawlerTypeUsed: string | null;
  sourceReachable: boolean;
  htmlFetched: boolean;
  linksScanned: number;
  pdfLinksFound: number;
  reasonWhenZeroResults: string | null;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface DiscoverySourceRun {
  id: string;
  sourceType: string;
  sourceUrl: string | null;
  status: string;
  discoveredCount: number;
  crawlerType: string | null;
  httpStatus: number | null;
  fetchedHtmlBytes: number;
  totalLinksFound: number;
  pdfLinksFound: number;
  skippedLinksCount: number;
  errorMessage: string | null;
  startedAt: string;
  completedAt: string | null;
}

export interface DiscoveryJobDetail {
  job: DiscoveryJob;
  sourceRuns: DiscoverySourceRun[];
}

export interface DiscoveredDocument {
  id: string;
  sourceType: string;
  sourceUrl: string | null;
  documentUrl: string;
  title: string;
  companySymbol: string | null;
  documentType: string;
  status: string;
  normalizedUrl: string;
  firstDiscoveredAt: string;
  lastSeenAt: string;
  seenCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface AutonomousPipelineStage {
  id: string;
  stageName: string;
  status: string;
  attemptCount: number;
  maxAttempts: number;
  durationMs: number;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface AutonomousPipelineEvent {
  id: string;
  pipelineStageId: string | null;
  eventType: string;
  message: string;
  details: string | null;
  createdAt: string;
}

export interface AutonomousPipelineJob {
  id: string;
  discoveredDocumentId: string | null;
  documentId: string | null;
  correlationId: string;
  status: string;
  currentStage: string | null;
  progressPercent: number;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  stages: AutonomousPipelineStage[];
  events: AutonomousPipelineEvent[];
}

export interface AutonomousPipelineMetrics {
  totalJobs: number;
  runningJobs: number;
  completedJobs: number;
  failedJobs: number;
  successRate: number;
  averageDurationMs: number;
}
