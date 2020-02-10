package com.github.anrimian.musicplayer.di.app.library;

import androidx.annotation.NonNull;

import com.github.anrimian.musicplayer.domain.business.library.LibraryFoldersInteractor;
import com.github.anrimian.musicplayer.domain.business.player.MusicPlayerInteractor;
import com.github.anrimian.musicplayer.domain.business.player.PlayerScreenInteractor;
import com.github.anrimian.musicplayer.domain.business.playlists.PlayListsInteractor;
import com.github.anrimian.musicplayer.domain.repositories.EditorRepository;
import com.github.anrimian.musicplayer.domain.repositories.LibraryRepository;
import com.github.anrimian.musicplayer.domain.repositories.MediaScannerRepository;
import com.github.anrimian.musicplayer.domain.repositories.PlayListsRepository;
import com.github.anrimian.musicplayer.domain.repositories.SettingsRepository;
import com.github.anrimian.musicplayer.domain.repositories.UiStateRepository;
import com.github.anrimian.musicplayer.ui.common.error.parser.ErrorParser;
import com.github.anrimian.musicplayer.ui.library.common.order.SelectOrderPresenter;
import com.github.anrimian.musicplayer.ui.player_screen.PlayerPresenter;
import com.github.anrimian.musicplayer.ui.settings.folders.ExcludedFoldersPresenter;

import javax.annotation.Nonnull;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;

import static com.github.anrimian.musicplayer.di.app.SchedulerModule.UI_SCHEDULER;

/**
 * Created on 29.10.2017.
 */
@Module
public class LibraryModule {

    @Provides
    @Nonnull
    PlayerPresenter playerPresenter(MusicPlayerInteractor musicPlayerInteractor,
                                    PlayListsInteractor playListsInteractor,
                                    PlayerScreenInteractor playerScreenInteractor,
                                    ErrorParser errorParser,
                                    @Named(UI_SCHEDULER) Scheduler uiScheduler) {
        return new PlayerPresenter(musicPlayerInteractor,
                playListsInteractor,
                playerScreenInteractor,
                errorParser,
                uiScheduler);
    }

    @Provides
    @NonNull
    PlayerScreenInteractor playerScreenInteractor(MusicPlayerInteractor musicPlayerInteractor,
                                                  UiStateRepository uiStateRepository,
                                                  SettingsRepository settingsRepository) {
        return new PlayerScreenInteractor(musicPlayerInteractor, uiStateRepository, settingsRepository);
    }

    @Provides
    @Nonnull
    SelectOrderPresenter selectOrderPresenter() {
        return new SelectOrderPresenter();
    }

    @Provides
    @Nonnull
    LibraryFoldersInteractor libraryFilesInteractor(LibraryRepository musicProviderRepository,
                                                    EditorRepository editorRepository,
                                                    MusicPlayerInteractor musicPlayerInteractor,
                                                    PlayListsRepository playListsRepository,
                                                    SettingsRepository settingsRepository,
                                                    UiStateRepository uiStateRepository,
                                                    MediaScannerRepository mediaScannerRepository) {
        return new LibraryFoldersInteractor(musicProviderRepository,
                editorRepository,
                musicPlayerInteractor,
                playListsRepository,
                settingsRepository,
                uiStateRepository,
                mediaScannerRepository);
    }

    @Provides
    @Nonnull
    ExcludedFoldersPresenter excludedFoldersPresenter(LibraryFoldersInteractor interactor,
                                                      @Named(UI_SCHEDULER) Scheduler uiScheduler,
                                                      ErrorParser errorParser) {
        return new ExcludedFoldersPresenter(interactor, uiScheduler, errorParser);
    }
}
