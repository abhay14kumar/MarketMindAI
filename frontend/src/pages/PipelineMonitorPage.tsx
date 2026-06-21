import {
  ErrorOutlineRounded,
  RefreshRounded,
  ReplayRounded,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Divider,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pipelineQueries } from '../api/client';
import type { PipelineRun } from '../api/types';
import { PageHeader } from '../components/PageHeader';
import { QueryState } from '../components/QueryState';
import { SectionCard } from '../components/SectionCard';
import { StatusChip } from '../components/StatusChip';
import { formatDateTime, formatEnum } from '../utils/format';

const stepOrder = [
  'DOWNLOAD',
  'TEXT_EXTRACTION',
  'CHUNKING',
  'EMBEDDING',
  'AI_READY',
] as const;

export function PipelineMonitorPage() {
  const queryClient = useQueryClient();
  const query = useQuery({
    queryKey: ['pipeline-runs'],
    queryFn: () => pipelineQueries.runs(0, 100),
    refetchInterval: 10_000,
  });
  const retry = useMutation({
    mutationFn: pipelineQueries.retry,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['pipeline-runs'] }),
  });
  const runs = query.data?.content ?? [];

  return (
    <>
      <PageHeader
        eyebrow="Document intelligence operations"
        title="Pipeline monitor"
        description="Follow each document from acquisition through text extraction, chunking, embedding, and AI readiness."
        action={(
          <Button
            variant="outlined"
            startIcon={<RefreshRounded />}
            onClick={() => void query.refetch()}
          >
            Refresh
          </Button>
        )}
      />
      {retry.error instanceof Error && (
        <Alert severity="error" sx={{ mb: 2 }}>{retry.error.message}</Alert>
      )}
      <SectionCard eyebrow="Automated processing" title="Recent runs">
        <QueryState
          loading={query.isPending}
          error={query.error instanceof Error ? query.error : null}
          empty={runs.length === 0}
          onRetry={() => void query.refetch()}
        >
          <Stack gap={2}>
            {runs.map((run) => (
              <PipelineRunCard
                key={run.id}
                run={run}
                retrying={retry.isPending && retry.variables === run.documentId}
                onRetry={() => retry.mutate(run.documentId)}
              />
            ))}
          </Stack>
        </QueryState>
      </SectionCard>
    </>
  );
}

function PipelineRunCard({
  run,
  retrying,
  onRetry,
}: {
  run: PipelineRun;
  retrying: boolean;
  onRetry: () => void;
}) {
  const stepByName = new Map(run.steps.map((step) => [step.stepName, step]));

  return (
    <Box sx={{ border: '1px solid', borderColor: 'divider', borderRadius: 2, p: 2 }}>
      <Stack
        direction={{ xs: 'column', md: 'row' }}
        justifyContent="space-between"
        gap={2}
      >
        <Box>
          <Typography fontWeight={700}>{run.documentTitle}</Typography>
          <Typography variant="caption" color="text.secondary">
            Started {formatDateTime(run.startedAt)} · Completed {formatDateTime(run.completedAt)}
          </Typography>
        </Box>
        <Stack direction="row" gap={1} alignItems="center">
          <StatusChip label={formatEnum(run.status)} />
          <Typography variant="caption" color="text.secondary">
            Current: {formatEnum(run.currentStep)}
          </Typography>
          {(run.status === 'FAILED' || run.status === 'PARTIAL') && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<ReplayRounded />}
              disabled={retrying}
              onClick={onRetry}
            >
              {retrying ? 'Retrying…' : 'Retry'}
            </Button>
          )}
        </Stack>
      </Stack>

      {run.status === 'STARTED' && <LinearProgress sx={{ mt: 2 }} />}
      {run.errorMessage && (
        <Alert icon={<ErrorOutlineRounded />} severity="error" sx={{ mt: 2 }}>
          {run.errorMessage}
        </Alert>
      )}

      <Divider sx={{ my: 2 }} />
      <Stack
        direction={{ xs: 'column', lg: 'row' }}
        gap={1}
        alignItems={{ lg: 'stretch' }}
      >
        {stepOrder.map((stepName, index) => {
          const step = stepByName.get(stepName);
          return (
            <Stack
              key={stepName}
              direction={{ xs: 'row', lg: 'column' }}
              gap={1}
              sx={{ flex: 1, minWidth: 0 }}
            >
              <Box
                sx={{
                  p: 1.5,
                  height: '100%',
                  borderRadius: 1.5,
                  bgcolor: 'rgba(255,255,255,0.025)',
                  border: '1px solid',
                  borderColor: 'divider',
                }}
              >
                <Typography variant="overline" color="text.secondary">
                  {index + 1}. {formatEnum(stepName)}
                </Typography>
                <Box sx={{ mt: 0.75 }}>
                  <StatusChip label={formatEnum(step?.status ?? 'PENDING')} />
                </Box>
                {step && (
                  <Typography variant="caption" color="text.secondary" display="block" mt={1}>
                    {formatDateTime(step.completedAt ?? step.startedAt)}
                    {step.retryCount > 0 ? ` · Retry ${step.retryCount}` : ''}
                  </Typography>
                )}
                {step?.errorMessage && (
                  <Typography variant="caption" color="error.main" display="block" mt={1}>
                    {step.errorMessage}
                  </Typography>
                )}
              </Box>
            </Stack>
          );
        })}
      </Stack>
    </Box>
  );
}
