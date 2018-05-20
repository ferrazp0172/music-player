package com.github.anrimian.simplemusicplayer.data.repositories.play_queue;

import com.github.anrimian.simplemusicplayer.data.models.exceptions.CompositionNotFoundException;
import com.github.anrimian.simplemusicplayer.data.preferences.UiStatePreferences;
import com.github.anrimian.simplemusicplayer.domain.models.composition.Composition;
import com.github.anrimian.simplemusicplayer.domain.models.composition.CurrentComposition;
import com.github.anrimian.simplemusicplayer.domain.repositories.PlayQueueRepository;

import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;

import static com.github.anrimian.simplemusicplayer.data.preferences.UiStatePreferences.NO_COMPOSITION;
import static com.github.anrimian.simplemusicplayer.data.utils.rx.RxUtils.withDefaultValue;
import static io.reactivex.subjects.BehaviorSubject.create;

/**
 * Created on 18.11.2017.
 */

public class PlayQueueRepositoryImpl implements PlayQueueRepository {

    private static final int NO_POSITION = 0;

    private final PlayQueueDataSourceNew playQueueDataSource;
    private final UiStatePreferences uiStatePreferences;
    private final Scheduler dbScheduler;

    private final BehaviorSubject<List<Composition>> currentPlayQueueSubject = create();
    private final BehaviorSubject<CurrentComposition> currentCompositionSubject = create();

    private int position = NO_POSITION;

    public PlayQueueRepositoryImpl(PlayQueueDataSourceNew playQueueDataSource,
                                   UiStatePreferences uiStatePreferences,
                                   Scheduler dbScheduler) {
        this.playQueueDataSource = playQueueDataSource;
        this.uiStatePreferences = uiStatePreferences;
        this.dbScheduler = dbScheduler;
    }

    @Override
    public Completable setPlayQueue(List<Composition> compositions) {
        checkCompositionsList(compositions);
        return playQueueDataSource.setPlayQueue(compositions)
                .doOnSuccess(playQueue -> {
                    currentPlayQueueSubject.onNext(playQueue);
                    position = 0;
                    updateCurrentComposition(playQueue, position);
                })
                .toCompletable()
                .subscribeOn(dbScheduler);
    }

    @Override
    public Observable<CurrentComposition> getCurrentCompositionObservable() {
        return withDefaultValue(currentCompositionSubject, getSavedComposition())
                .subscribeOn(dbScheduler);
    }

    @Override
    public Single<CurrentComposition> getCurrentComposition() {
        return getCurrentCompositionObservable()
                .lastOrError()
                .onErrorResumeNext(Single.error(new CompositionNotFoundException()));
    }

    @Override
    public Observable<List<Composition>> getPlayQueueObservable() {
        return withDefaultValue(currentPlayQueueSubject, playQueueDataSource.getPlayQueue())
                .subscribeOn(dbScheduler);
    }

    @Override
    public void setRandomPlayingEnabled(boolean enabled) {
        CurrentComposition currentComposition = currentCompositionSubject.getValue();
        if (currentComposition == null) {
            throw new IllegalStateException("change play mode without current composition");
        }

        playQueueDataSource.setRandomPlayingEnabled(enabled, currentComposition.getComposition())
                .flatMap(position -> playQueueDataSource.getPlayQueue()
                        .doOnSuccess(playQueue -> {
                            this.position = position;

                            currentPlayQueueSubject.onNext(playQueue);
                            uiStatePreferences.setCurrentCompositionPosition(position);
                        }))
                .subscribe();
    }

    @Override
    public Single<Integer> skipToNext() {
        return playQueueDataSource.getPlayQueue()
                .map(currentPlayList -> {
                    checkCompositionsList(currentPlayList);

                    if (position >= currentPlayList.size() - 1) {
                        position = 0;
                    } else {
                        position++;
                    }
                    updateCurrentComposition(currentPlayList, position);
                    return position;
                });
    }

    @Override
    public Single<Integer> skipToPrevious() {
        return playQueueDataSource.getPlayQueue()
                .map(currentPlayList -> {
                    checkCompositionsList(currentPlayList);

                    position--;
                    if (position < 0) {
                        position = currentPlayList.size() - 1;
                    }
                    updateCurrentComposition(currentPlayList, position);
                    return position;
                });
    }

    @Override
    public Completable skipToPosition(int position) {
        return playQueueDataSource.getPlayQueue()
                .doOnSuccess(currentPlayList -> {
                    checkCompositionsList(currentPlayList);

                    if (position < 0 || position >= currentPlayList.size()) {
                        throw new IndexOutOfBoundsException("unexpected position: " + position);
                    }

                    this.position = position;
                    updateCurrentComposition(currentPlayList, position);
                })
                .toCompletable();
    }

    private void updateCurrentComposition(List<Composition> currentPlayList, int position) {
        Composition composition = currentPlayList.get(position);
        uiStatePreferences.setCurrentCompositionId(composition.getId());
        uiStatePreferences.setCurrentCompositionPosition(position);

        CurrentComposition currentComposition = new CurrentComposition(composition, position, 0);
        currentCompositionSubject.onNext(currentComposition);
    }

    private Maybe<CurrentComposition> getSavedComposition() {
        return playQueueDataSource.getPlayQueue()
                .flatMapMaybe(this::findSavedComposition);
    }

    @Nullable
    private Maybe<CurrentComposition> findSavedComposition(List<Composition> compositions) {
        return Maybe.create(emitter -> {
            long id = uiStatePreferences.getCurrentCompositionId();
            int position = uiStatePreferences.getCurrentCompositionPosition();

            //optimized way
            if (position > 0 && position < compositions.size()) {
                Composition expectedComposition = compositions.get(position);
                if (expectedComposition.getId() == id) {
                    this.position = position;//TODO maybe remove position? replace with hash map?
                    emitter.onSuccess(new CurrentComposition(expectedComposition,
                            position,
                            uiStatePreferences.getTrackPosition()));
                    return;
                }
            }

            if (id == NO_COMPOSITION) {
                emitter.onComplete();
                return;
            }

            for (int i = 0; i< compositions.size(); i++) {
                Composition composition = compositions.get(i);
                if (composition.getId() == id) {
                    this.position = i;
                    emitter.onSuccess(new CurrentComposition(composition,
                            position,
                            uiStatePreferences.getTrackPosition()));
                    return;
                }
            }
            emitter.onComplete();
        });
    }

    private void checkCompositionsList(@Nullable List<Composition> compositions) {
        if (compositions == null || compositions.isEmpty()) {
            throw new IllegalStateException("empty play queue");
        }
    }
}