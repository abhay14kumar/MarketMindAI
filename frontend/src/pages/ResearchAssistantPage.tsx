import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  AutoAwesomeRounded,
  DataObjectRounded,
  DescriptionRounded,
  SendRounded,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { aiQueries, documentQueries } from '../api/client';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { formatDateTime, formatEnum } from '../utils/format';

export function ResearchAssistantPage() {
  const queryClient = useQueryClient();
  const [question, setQuestion] = useState('');
  const [documentId, setDocumentId] = useState('');
  const [topK, setTopK] = useState(5);
  const documentsQuery = useQuery({
    queryKey: ['documents'],
    queryFn: documentQueries.list,
  });
  const historyQuery = useQuery({
    queryKey: ['ai', 'answers'],
    queryFn: aiQueries.answers,
  });
  const chunksQuery = useQuery({
    queryKey: ['ai', 'chunks', documentId],
    queryFn: () => aiQueries.chunks(documentId),
    enabled: Boolean(documentId),
  });
  const askMutation = useMutation({
    mutationFn: aiQueries.ask,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ai', 'answers'] });
    },
  });
  const embedMutation = useMutation({
    mutationFn: aiQueries.embedDocument,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['ai', 'chunks', documentId] });
    },
  });

  const documents = documentsQuery.data?.data ?? [];
  const answer = askMutation.data;

  return (
    <>
      <PageHeader
        eyebrow="AI research assistant"
        title="Evidence-grounded workspace"
        description="Ask questions against extracted and indexed documents using local Ollama and Qdrant."
      />
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <SectionCard eyebrow="Research session" title="Document Q&A" minHeight={620}>
            <Stack spacing={2.2}>
              <Stack direction={{ xs: 'column', md: 'row' }} gap={1.5}>
                <TextField
                  select
                  label="Document scope"
                  value={documentId}
                  onChange={(event) => setDocumentId(event.target.value)}
                  sx={{ flex: 1 }}
                >
                  <MenuItem value="">All indexed documents</MenuItem>
                  {documents.map((document) => (
                    <MenuItem key={document.id} value={document.id}>
                      {document.title}
                    </MenuItem>
                  ))}
                </TextField>
                <TextField
                  select
                  label="Top K"
                  value={topK}
                  onChange={(event) => setTopK(Number(event.target.value))}
                  sx={{ width: 110 }}
                >
                  {[3, 5, 8, 10].map((value) => (
                    <MenuItem key={value} value={value}>{value}</MenuItem>
                  ))}
                </TextField>
                <Button
                  variant="outlined"
                  startIcon={embedMutation.isPending
                    ? <CircularProgress size={15} color="inherit" />
                    : <DataObjectRounded />}
                  disabled={!documentId || embedMutation.isPending}
                  onClick={() => embedMutation.mutate(documentId)}
                >
                  {embedMutation.isPending ? 'Indexing…' : 'Index document'}
                </Button>
              </Stack>

              {embedMutation.data && (
                <Alert severity={embedMutation.data.status === 'COMPLETED' ? 'success' : 'warning'}>
                  {formatEnum(embedMutation.data.status)}: {embedMutation.data.embeddedChunks}
                  {' '}of {embedMutation.data.totalChunks} chunks indexed.
                  {embedMutation.data.errorMessage && ` ${embedMutation.data.errorMessage}`}
                </Alert>
              )}
              {embedMutation.error instanceof Error && (
                <Alert severity="error">{embedMutation.error.message}</Alert>
              )}

              <TextField
                multiline
                minRows={4}
                value={question}
                onChange={(event) => setQuestion(event.target.value)}
                placeholder="Ask a question grounded in indexed annual reports, results, or transcripts…"
              />
              <Stack direction="row" justifyContent="space-between" alignItems="center" gap={2}>
                <Typography variant="caption" color="text.secondary">
                  AI answer is based only on indexed documents and is not financial advice.
                </Typography>
                <Button
                  variant="contained"
                  endIcon={askMutation.isPending
                    ? <CircularProgress size={15} color="inherit" />
                    : <SendRounded />}
                  disabled={!question.trim() || askMutation.isPending}
                  onClick={() => askMutation.mutate({
                    question: question.trim(),
                    documentId: documentId || undefined,
                    topK,
                  })}
                >
                  {askMutation.isPending ? 'Searching…' : 'Ask MarketMind'}
                </Button>
              </Stack>
              {askMutation.error instanceof Error && (
                <Alert severity="error">{askMutation.error.message}</Alert>
              )}

              {answer && (
                <>
                  <Divider />
                  <Box sx={{ p: 2.25, borderRadius: 2, bgcolor: 'rgba(84,214,194,0.055)', border: '1px solid', borderColor: 'divider' }}>
                    <Stack direction="row" justifyContent="space-between" gap={2} flexWrap="wrap">
                      <Typography variant="overline" color="primary.main">Grounded answer</Typography>
                      <Stack direction="row" gap={1}>
                        <Chip label={formatEnum(answer.status)} variant="outlined" />
                        <Chip label={`Confidence ${Math.round(answer.confidenceScore * 100)}%`} variant="outlined" />
                      </Stack>
                    </Stack>
                    <Typography sx={{ mt: 1.2, whiteSpace: 'pre-wrap', lineHeight: 1.8 }}>
                      {answer.answer}
                    </Typography>
                  </Box>
                  <Stack gap={1.25}>
                    <Typography variant="h3">Citations and retrieved evidence</Typography>
                    {answer.citations.length === 0 ? (
                      <Alert severity="info">No sufficiently relevant indexed context was found.</Alert>
                    ) : answer.citations.map((citation) => (
                      <Box key={citation.chunkId} sx={{ p: 1.7, border: '1px solid', borderColor: 'divider', borderRadius: 1.5 }}>
                        <Stack direction="row" gap={1} alignItems="center" mb={0.8}>
                          <DescriptionRounded color="primary" fontSize="small" />
                          <Typography variant="overline" color="text.secondary">
                            Chunk {citation.chunkIndex} · {citation.documentId}
                          </Typography>
                        </Stack>
                        <Typography variant="body2" sx={{ lineHeight: 1.7 }}>{citation.snippet}</Typography>
                      </Box>
                    ))}
                  </Stack>
                </>
              )}
            </Stack>
          </SectionCard>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <Stack spacing={2}>
            <SectionCard eyebrow="Index status" title="Selected document">
              {!documentId ? (
                <Typography color="text.secondary">Select a document to inspect or build its index.</Typography>
              ) : (
                <QueryState
                  loading={chunksQuery.isPending}
                  error={chunksQuery.error instanceof Error ? chunksQuery.error : null}
                  empty={(chunksQuery.data?.length ?? 0) === 0}
                  onRetry={() => void chunksQuery.refetch()}
                  emptyMessage="No chunks indexed for this document."
                >
                  <Stack gap={1}>
                    <Typography>{chunksQuery.data?.length ?? 0} persisted chunks</Typography>
                    <Typography variant="caption" color="text.secondary">
                      Collection: {chunksQuery.data?.[0]?.qdrantCollection ?? 'Not indexed'}
                    </Typography>
                  </Stack>
                </QueryState>
              )}
            </SectionCard>
            <SectionCard eyebrow="Recent research" title="Question history">
              <QueryState
                loading={historyQuery.isPending}
                error={historyQuery.error instanceof Error ? historyQuery.error : null}
                empty={(historyQuery.data?.length ?? 0) === 0}
                onRetry={() => void historyQuery.refetch()}
                emptyMessage="No grounded questions have been asked yet."
              >
                <Stack gap={1.2}>
                  {historyQuery.data?.slice(0, 6).map((item) => (
                    <Box key={item.id} sx={{ p: 1.3, border: '1px solid', borderColor: 'divider', borderRadius: 1.5 }}>
                      <Stack direction="row" gap={1} alignItems="center">
                        <AutoAwesomeRounded color="primary" fontSize="small" />
                        <Typography variant="body2" fontWeight={650}>{item.question}</Typography>
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        {formatEnum(item.status)} · {formatDateTime(item.createdAt)}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              </QueryState>
            </SectionCard>
          </Stack>
        </Grid>
      </Grid>
    </>
  );
}
