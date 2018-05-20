package com.github.anrimian.simplemusicplayer.data.repositories.play_queue;

import com.github.anrimian.simplemusicplayer.data.database.dao.PlayQueueDao;
import com.github.anrimian.simplemusicplayer.data.database.models.PlayQueueEntity;
import com.github.anrimian.simplemusicplayer.data.preferences.SettingsPreferences;
import com.github.anrimian.simplemusicplayer.data.storage.StorageMusicDataSource;
import com.github.anrimian.simplemusicplayer.domain.models.composition.Composition;
import com.github.anrimian.simplemusicplayer.domain.utils.changes.ChangeableMap;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;

import static com.github.anrimian.simplemusicplayer.data.TestDataProvider.getFakeCompositions;
import static com.github.anrimian.simplemusicplayer.data.TestDataProvider.getFakeCompositionsMap;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayQueueDataSourceTest {

    private final PlayQueueDao playQueueDao = mock(PlayQueueDao.class);
    private final StorageMusicDataSource storageMusicDataSource = mock(StorageMusicDataSource.class);
    private final SettingsPreferences settingsPreferences = mock(SettingsPreferences.class);
    private final Scheduler scheduler = Schedulers.trampoline();

    private PlayQueueDataSourceNew playQueueDataSource;

    @Before
    public void setUp() {
        when(settingsPreferences.isRandomPlayingEnabled()).thenReturn(false);
        when(storageMusicDataSource.getCompositionsList()).thenReturn(new ChangeableMap<>(
                getFakeCompositionsMap(), Observable.never()));

        playQueueDataSource = new PlayQueueDataSourceNew(playQueueDao,
                storageMusicDataSource,
                settingsPreferences,
                scheduler);
    }

    @Test
    public void setPlayQueueInNormalMode() {
        playQueueDataSource.setPlayQueue(getFakeCompositions())
                .test()
                .assertValue(compositions -> {
                    assertEquals(getFakeCompositions(), compositions);
                    return true;
                });

        verify(playQueueDao).deletePlayQueue();
        verify(playQueueDao).setPlayQueue(anyListOf(PlayQueueEntity.class));
    }

    @Test
    public void setPlayQueueInShuffleMode() {
        when(settingsPreferences.isRandomPlayingEnabled()).thenReturn(true);

        playQueueDataSource.setPlayQueue(getFakeCompositions())
                .test()
                .assertValue(compositions -> {
                    assertNotEquals(getFakeCompositions(), compositions);
                    assertEquals(getFakeCompositions().size(), compositions.size());
                    return true;
                });
    }

    @Test
    public void getEmptyPlayQueueInInitialState() {
        when(playQueueDao.getPlayQueue()).thenReturn(emptyList());

        playQueueDataSource.getPlayQueue()
                .test()
                .assertValue(compositions -> {
                    assertEquals(0, compositions.size());
                    return true;
                });
    }

    @Test
    public void getPlayQueueInInitialState() {
        when(playQueueDao.getPlayQueue()).thenReturn(getPlayQueueEntities());

        playQueueDataSource.getPlayQueue()
                .test()
                .assertValue(compositions -> {
                    assertEquals(getPlayQueueEntities().size(), compositions.size());
                    assertEquals(getFakeCompositions(), compositions);
                    return true;
                });
    }

    @Test
    public void getPlayQueueWithUnexcitingCompositions() {
        PlayQueueEntity playQueueEntity = new PlayQueueEntity();
        playQueueEntity.setId(Long.MAX_VALUE);
        playQueueEntity.setPosition(0);
        playQueueEntity.setShuffledPosition(0);
        when(playQueueDao.getPlayQueue()).thenReturn(singletonList(playQueueEntity));

        playQueueDataSource.getPlayQueue()
                .test()
                .assertValue(compositions -> {
                    assertEquals(0, compositions.size());
                    return true;
                });

        verify(playQueueDao).deletePlayQueueEntity(eq(Long.MAX_VALUE));
    }

    @Test
    public void setRandomPlayingDisabledTest() {
        playQueueDataSource.setPlayQueue(getFakeCompositions()).subscribe();

        when(settingsPreferences.isRandomPlayingEnabled()).thenReturn(false);

        playQueueDataSource.setRandomPlayingEnabled(false, getFakeCompositions().get(1))
                .test()
                .assertValue(1);
    }

    @Test
    public void setRandomPlayingEnabledTest() {
        playQueueDataSource.setPlayQueue(getFakeCompositions()).subscribe();

        when(settingsPreferences.isRandomPlayingEnabled()).thenReturn(true);

        Composition composition = getFakeCompositions().get(1);
        playQueueDataSource.setRandomPlayingEnabled(true, composition)
                .test()
                .assertValue(0);

        playQueueDataSource.getPlayQueue()
                .flatMapObservable(Observable::fromIterable)
                .test()
                .assertComplete();

        verify(playQueueDao).updatePlayQueue(anyListOf(PlayQueueEntity.class));
    }

    private List<PlayQueueEntity> getPlayQueueEntities() {
        List<PlayQueueEntity> playQueueEntities = new ArrayList<>();
        List<Composition> compositions = getFakeCompositions();
        for (int i = 0; i < 100000; i++) {
            Composition composition = compositions.get(i);
            PlayQueueEntity playQueueEntity = new PlayQueueEntity();
            playQueueEntity.setId(composition.getId());
            playQueueEntity.setPosition(i);
            playQueueEntity.setShuffledPosition(compositions.size() - 1 - i);

            playQueueEntities.add(playQueueEntity);
        }
        return playQueueEntities;
    }
}