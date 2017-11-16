package com.github.anrimian.simplemusicplayer.domain.business.player;

import com.github.anrimian.simplemusicplayer.domain.controllers.MusicPlayerController;
import com.github.anrimian.simplemusicplayer.domain.controllers.MusicServiceController;
import com.github.anrimian.simplemusicplayer.domain.models.Composition;
import com.github.anrimian.simplemusicplayer.domain.models.player.InternalPlayerState;
import com.github.anrimian.simplemusicplayer.domain.models.player.PlayerState;
import com.github.anrimian.simplemusicplayer.domain.models.player.TrackState;
import com.github.anrimian.simplemusicplayer.domain.repositories.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.PublishSubject;

import static com.github.anrimian.simplemusicplayer.domain.models.player.PlayerState.IDLE;
import static com.github.anrimian.simplemusicplayer.domain.models.player.PlayerState.LOADING;
import static com.github.anrimian.simplemusicplayer.domain.models.player.PlayerState.PAUSE;
import static com.github.anrimian.simplemusicplayer.domain.models.player.PlayerState.PLAY;
import static com.github.anrimian.simplemusicplayer.domain.models.player.PlayerState.STOP;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created on 12.11.2017.
 */
public class MusicPlayerInteractorTest {

    private MusicPlayerInteractor musicPlayerInteractor;
    private MusicPlayerController musicPlayerController;
    private MusicServiceController musicServiceController;

    private SettingsRepository settingsRepository;

    private TestObserver<PlayerState> playerStateTestObserver = new TestObserver<>();
    private TestObserver<Composition> currentCompositionTestObserver = new TestObserver<>();
    private TestObserver<List<Composition>> currentPlayListTestObserver = new TestObserver<>();
    private TestObserver<TrackState> trackStateTestObserver = new TestObserver<>();

    private Composition one = new Composition();
    private Composition two = new Composition();
    private Composition three = new Composition();
    private Composition four = new Composition();

    private List<Composition> fakeCompositions = new ArrayList<>();

    private PublishSubject<InternalPlayerState> internalPlayerStateObservable = PublishSubject.create();
    private PublishSubject<TrackState> trackStateObservable = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        one.setFilePath("root/music/one");
        one.setId(1);
        fakeCompositions.add(one);

        two.setFilePath("root/music/two");
        two.setId(2);
        fakeCompositions.add(two);

        three.setFilePath("root/music/old/three");
        three.setId(3);
        fakeCompositions.add(three);

        four.setFilePath("root/music/old/to delete/four");
        four.setId(4);
        fakeCompositions.add(four);

        musicPlayerController = mock(MusicPlayerController.class);
        when(musicPlayerController.getPlayerStateObservable()).thenReturn(internalPlayerStateObservable);
        when(musicPlayerController.getTrackStateObservable()).thenReturn(trackStateObservable);
        when(musicPlayerController.play(any())).thenReturn(Completable.complete());
        musicServiceController = mock(MusicServiceController.class);

        settingsRepository = mock(SettingsRepository.class);
        when(settingsRepository.isInfinitePlayingEnabled()).thenReturn(false);
        when(settingsRepository.isRandomPlayingEnabled()).thenReturn(false);

        musicPlayerInteractor = new MusicPlayerInteractor(musicPlayerController, musicServiceController, settingsRepository);
        musicPlayerInteractor.getPlayerStateObservable().subscribe(playerStateTestObserver);
        musicPlayerInteractor.getCurrentCompositionObservable().subscribe(currentCompositionTestObserver);
        musicPlayerInteractor.getCurrentPlayListObservable().subscribe(currentPlayListTestObserver);
        musicPlayerInteractor.getTrackStateObservable().subscribe(trackStateTestObserver);
    }

    @Test
    public void startPlayingTest() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);

        verify(musicServiceController).start();
        playerStateTestObserver.assertValues(IDLE, LOADING, PLAY);
        currentCompositionTestObserver.assertValues(one);
        currentPlayListTestObserver.assertValue(fakeCompositions);
        verify(musicPlayerController).play(eq(one));
    }

    @Test
    public void emptyPlayTest() throws Exception {
        musicPlayerInteractor.play();

        verify(musicServiceController, never()).start();
        verify(musicPlayerController, never()).resume();
        playerStateTestObserver.assertValues(IDLE);
    }

    @Test
    public void playAfterPauseTest() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);
        musicPlayerInteractor.pause();
        musicPlayerInteractor.play();

        verify(musicServiceController, times(2)).start();
        verify(musicPlayerController).resume();
        playerStateTestObserver.assertValues(IDLE, LOADING, PLAY, PAUSE, PLAY);
    }

    @Test
    public void playAfterStopTest() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);
        musicPlayerInteractor.stop();
        musicPlayerInteractor.play();

        verify(musicServiceController, times(2)).start();
        verify(musicPlayerController, times(2)).play(eq(one));
        playerStateTestObserver.assertValues(IDLE, LOADING, PLAY, STOP, LOADING, PLAY);
    }

    @Test
    public void stopTest() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);
        musicPlayerInteractor.stop();

        verify(musicPlayerController).stop();
        playerStateTestObserver.assertValues(IDLE, LOADING, PLAY, STOP);
    }

    @Test
    public void pauseTest() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);
        musicPlayerInteractor.pause();

        verify(musicPlayerController).stop();
        playerStateTestObserver.assertValues(IDLE, LOADING, PLAY, PAUSE);
    }

    @Test
    public void testSkipToNext() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);

        musicPlayerInteractor.skipToNext();
        verify(musicPlayerController).play(eq(two));
    }

    @Test
    public void testSkipToPrevious() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);

        musicPlayerInteractor.skipToPrevious();
        verify(musicPlayerController).play(eq(four));
    }

    @Test
    public void testSkipToNextInPauseState() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);
        musicPlayerInteractor.pause();
        musicPlayerInteractor.skipToNext();
        musicPlayerInteractor.play();

        verify(musicPlayerController).play(eq(two));
    }

    @Test
    public void testAutoPlayWithoutRepeat() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);

        internalPlayerStateObservable.onNext(InternalPlayerState.ENDED);
        internalPlayerStateObservable.onNext(InternalPlayerState.ENDED);
        internalPlayerStateObservable.onNext(InternalPlayerState.ENDED);
        internalPlayerStateObservable.onNext(InternalPlayerState.ENDED);
        internalPlayerStateObservable.onNext(InternalPlayerState.ENDED);
        internalPlayerStateObservable.onNext(InternalPlayerState.ENDED);
        internalPlayerStateObservable.onNext(InternalPlayerState.ENDED);

        verify(musicPlayerController, times(4)).play(any());
    }

    @Test
    public void testRandomPlaying() throws Exception {
        musicPlayerInteractor.startPlaying(fakeCompositions);

        when(settingsRepository.isRandomPlayingEnabled()).thenReturn(true);
        musicPlayerInteractor.setRandomPlayingEnabled(true);

        musicPlayerInteractor.skipToNext();
        musicPlayerInteractor.skipToPrevious();
        verify(musicPlayerController, times(2)).play(eq(one));
    }

}