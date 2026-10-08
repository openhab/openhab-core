/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.audio.internal.transcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioStream;
import org.openhab.core.audio.transcode.AudioTranscoder;
import org.openhab.core.audio.transcode.AudioTranscodingException;
import org.openhab.core.audio.transcode.AudioTranscodingService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link AudioTranscodingService} that dynamically tracks {@link AudioTranscoder}
 * components and resolves transcoding paths.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = AudioTranscodingService.class)
@NonNullByDefault
public class AudioTranscodingServiceImpl implements AudioTranscodingService {
    private static final int MAX_STEPS = 5;

    private final Logger logger = LoggerFactory.getLogger(AudioTranscodingServiceImpl.class);

    private final Set<AudioTranscoder> transcoders = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addAudioTranscoder(AudioTranscoder transcoder) {
        logger.debug("Adding audio transcoder: {}", transcoder.getId());
        transcoders.add(transcoder);
    }

    public void removeAudioTranscoder(AudioTranscoder transcoder) {
        logger.debug("Removing audio transcoder: {}", transcoder.getId());
        transcoders.remove(transcoder);
    }

    @Override
    public boolean canTranscode(AudioFormat sourceFormat, AudioFormat targetFormat) {
        if (targetFormat.isCompatible(sourceFormat)) {
            return true;
        }

        return findTranscodingPath(sourceFormat, List.of(targetFormat)) != null;
    }

    @Override
    public AudioStream transcode(AudioStream source, AudioFormat targetFormat) throws AudioTranscodingException {
        if (targetFormat.isCompatible(source.getFormat())) {
            return source;
        }

        TranscodingPlan plan = findTranscodingPath(source.getFormat(), List.of(targetFormat));
        if (plan == null) {
            throw new AudioTranscodingException(
                    "No transcoding path available from " + source.getFormat() + " to " + targetFormat);
        }

        return transcode(source, plan);
    }

    @Override
    public AudioStream transcodeToSupported(AudioStream source, Set<AudioFormat> candidateFormats)
            throws AudioTranscodingException {
        for (AudioFormat candidate : candidateFormats) {
            if (candidate.isCompatible(source.getFormat())) {
                return source;
            }
        }

        TranscodingPlan plan = findTranscodingPath(source.getFormat(), candidateFormats);
        if (plan == null) {
            throw new AudioTranscodingException("No transcoding path available from format " + source.getFormat()
                    + " to any of " + candidateFormats);
        }

        return transcode(source, plan);
    }

    private AudioStream transcode(AudioStream source, TranscodingPlan plan) throws AudioTranscodingException {
        AudioStream currentStream = source;
        for (TranscodingStep step : plan.steps()) {
            logger.debug("Executing transcoding step with '{}' towards format '{}'", step.transcoder().getId(),
                    step.targetFormat());
            currentStream = step.transcoder().transcode(currentStream, step.targetFormat());
        }
        return currentStream;
    }

    /**
     * Finds a transcoding path from the given start format to the given goal formats using iterative depth-first search
     * (DFS) with an explicit stack.
     *
     * @param start
     * @param goals
     * @return
     */
    private @Nullable TranscodingPlan findTranscodingPath(AudioFormat start, Collection<AudioFormat> goals) {
        AudioTranscoder[] all = transcoders.toArray(new AudioTranscoder[0]);
        AudioFormat[] goalArr = goals.toArray(new AudioFormat[0]);

        // per-depth search state (the explicit stack)
        int[] ti = new int[MAX_STEPS]; // current transcoder index
        int[] oi = new int[MAX_STEPS]; // current output index
        int[] costBefore = new int[MAX_STEPS];
        AudioFormat[] from = new AudioFormat[MAX_STEPS];
        List<List<AudioFormat>> candidatesAtDepth = new ArrayList<>(MAX_STEPS);
        for (int i = 0; i < MAX_STEPS; i++) {
            candidatesAtDepth.add(List.of());
        }
        // current path
        AudioTranscoder[] pathT = new AudioTranscoder[MAX_STEPS];
        AudioFormat[] pathF = new AudioFormat[MAX_STEPS];

        int bestCost = Integer.MAX_VALUE;
        int bestLength = Integer.MAX_VALUE;
        AudioFormat bestGoal = null;
        List<TranscodingStep> bestSteps = null;

        from[0] = start;
        int depth = 0;
        while (depth >= 0) {
            if (ti[depth] >= all.length) {
                candidatesAtDepth.set(depth, List.of());
                depth--; // this level is exhausted, the parent resumes with its already advanced index
                continue;
            }
            AudioTranscoder transcoder = all[ti[depth]];
            int next = costBefore[depth] + transcoder.getCost();

            if (next > bestCost) {
                ti[depth]++; // next transcoder
                oi[depth] = 0;
                candidatesAtDepth.set(depth, List.of());
                continue;
            }

            List<AudioFormat> candidates = candidatesAtDepth.get(depth);
            if (candidates.isEmpty()) {
                candidates = getCandidateOutputs(transcoder, from[depth], goalArr);
                candidatesAtDepth.set(depth, candidates);
            }

            if (oi[depth] >= candidates.size()) {
                ti[depth]++; // next transcoder
                oi[depth] = 0;
                candidatesAtDepth.set(depth, List.of());
                continue;
            }

            AudioFormat out = candidates.get(oi[depth]++);
            boolean createsCycle = false;
            for (int i = 0; i <= depth; i++) {
                if (from[i].equals(out)) {
                    createsCycle = true;
                    break;
                }
            }
            if (createsCycle) {
                continue;
            }
            if (!transcoder.canTranscode(from[depth], out)) {
                continue;
            }
            pathT[depth] = transcoder;
            pathF[depth] = out;

            AudioFormat hit = null;
            for (AudioFormat goal : goalArr) {
                if (goal.isCompatible(out)) {
                    hit = goal;
                    break;
                }
            }

            if (hit != null) {
                int length = depth + 1;
                if (next < bestCost || (next == bestCost && length < bestLength)) {
                    bestCost = next;
                    bestLength = length;
                    bestGoal = hit;
                    List<TranscodingStep> steps = new ArrayList<>(length);
                    for (int i = 0; i < length; i++) {
                        steps.add(new TranscodingStep(pathT[i], pathF[i]));
                    }
                    bestSteps = steps;
                }
            } else if (depth + 1 < MAX_STEPS) {
                depth++; // descend
                ti[depth] = 0;
                oi[depth] = 0;
                candidatesAtDepth.set(depth, List.of());
                costBefore[depth] = next;
                from[depth] = out;
            }
        }

        return bestSteps == null || bestGoal == null ? null : new TranscodingPlan(bestGoal, bestSteps);
    }

    private List<AudioFormat> getCandidateOutputs(AudioTranscoder transcoder, AudioFormat current,
            AudioFormat[] goals) {
        List<AudioFormat> candidates = new ArrayList<>();

        // Direct goals that match the transcoder's supported target formats
        for (AudioFormat goal : goals) {
            for (AudioFormat target : transcoder.getSupportedTargetFormats()) {
                if (target.isCompatible(goal)) {
                    candidates.add(goal);
                    break;
                }
            }
        }

        // Transcoder-specific intermediate targets
        if (PcmResampler.ID.equals(transcoder.getId())) {
            // For PCM resampler, generate PCM target formats matching frequencies and/or bit depths requested by goals
            for (AudioFormat goal : goals) {
                Long targetFreq = goal.getFrequency() != null ? goal.getFrequency() : current.getFrequency();
                Integer targetBitDepth = goal.getBitDepth() != null ? goal.getBitDepth() : current.getBitDepth();
                Boolean targetBigEndian = goal.isBigEndian() != null ? goal.isBigEndian() : current.isBigEndian();

                boolean freqChanges = goal.getFrequency() != null
                        && (current.getFrequency() == null || !goal.getFrequency().equals(current.getFrequency()));
                boolean bitDepthChanges = goal.getBitDepth() != null
                        && (current.getBitDepth() == null || !goal.getBitDepth().equals(current.getBitDepth()));
                boolean endiannessChanges = goal.isBigEndian() != null && (current.isBigEndian() == null
                        || !Objects.equals(goal.isBigEndian(), current.isBigEndian()));

                if (freqChanges || bitDepthChanges || endiannessChanges) {
                    AudioFormat resampled = new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_SIGNED,
                            targetBigEndian, targetBitDepth, null, targetFreq, current.getChannels());
                    if (!candidates.contains(resampled)) {
                        candidates.add(resampled);
                    }
                }
            }
        } else {
            for (AudioFormat target : transcoder.getSupportedTargetFormats()) {
                AudioFormat concrete = new AudioFormat(
                        target.getContainer() != null ? target.getContainer() : current.getContainer(),
                        target.getCodec() != null ? target.getCodec() : current.getCodec(),
                        target.isBigEndian() != null ? target.isBigEndian() : current.isBigEndian(),
                        target.getBitDepth() != null ? target.getBitDepth() : current.getBitDepth(), null,
                        target.getFrequency() != null ? target.getFrequency() : current.getFrequency(),
                        current.getChannels() != null ? current.getChannels() : target.getChannels());
                if (!candidates.contains(concrete)) {
                    candidates.add(concrete);
                }
            }
        }

        return candidates;
    }

    private record TranscodingStep(AudioTranscoder transcoder, AudioFormat targetFormat) {
    }

    private record TranscodingPlan(AudioFormat target, List<TranscodingStep> steps) {
    }
}
