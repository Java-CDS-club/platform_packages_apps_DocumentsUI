/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui;

import android.app.Fragment;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Menus;
import com.android.documentsui.base.RootInfo;
import com.android.documentsui.base.Shared;
import com.android.documentsui.base.State;
import com.android.documentsui.dirlist.ActionModeController;
import com.android.documentsui.dirlist.DirectoryFragment;
import com.android.documentsui.sidebar.RootsFragment;
import com.android.internal.annotations.VisibleForTesting;

import java.util.List;
import java.util.function.IntFunction;

public abstract class MenuManager {

    final protected SearchViewManager mSearchManager;
    final protected State mState;
    final protected DirectoryDetails mDirDetails;

    public MenuManager(
            SearchViewManager searchManager,
            State displayState,
            DirectoryDetails dirDetails) {
        mSearchManager = searchManager;
        mState = displayState;
        mDirDetails = dirDetails;
    }

    /** @see ActionModeController */
    public void updateActionMenu(Menu menu, SelectionDetails selection) {
        updateOpenInActionMode(menu.findItem(R.id.menu_open), selection);
        updateDelete(menu.findItem(R.id.menu_delete), selection);
        updateShare(menu.findItem(R.id.menu_share), selection);
        updateRename(menu.findItem(R.id.menu_rename), selection);
        updateSelectAll(menu.findItem(R.id.menu_select_all));
        updateMoveTo(menu.findItem(R.id.menu_move_to), selection);
        updateCopyTo(menu.findItem(R.id.menu_copy_to), selection);

        Menus.disableHiddenItems(menu);
    }

    /** @see BaseActivity#onPrepareOptionsMenu */
    public void updateOptionMenu(Menu menu) {
        updateCreateDir(menu.findItem(R.id.menu_create_dir));
        updateSettings(menu.findItem(R.id.menu_settings));
        updateNewWindow(menu.findItem(R.id.menu_new_window));
        updateModePicker(menu.findItem(R.id.menu_grid), menu.findItem(R.id.menu_list));
        // Sort menu item is managed by SortMenuManager
        updateAdvanced(menu.findItem(R.id.menu_advanced));

        Menus.disableHiddenItems(menu);
    }

    /**
     * Called when we needs {@link MenuManager} to ask Android to show context menu for us.
     * {@link MenuManager} can choose to defeat this request.
     *
     * {@link #inflateContextMenuForDocs} and {@link #inflateContextMenuForContainer} are called
     * afterwards when Android asks us to provide the content of context menus, so they're not
     * correct locations to suppress context menus.
     */
    public void showContextMenu(Fragment f, View v, float x, float y) {
        // Pickers don't have any context menu at this moment.
    }

    public void inflateContextMenuForContainer(Menu menu, MenuInflater inflater) {
        throw new UnsupportedOperationException("Pickers don't allow context menu.");
    }

    public void inflateContextMenuForDocs(
            Menu menu, MenuInflater inflater, SelectionDetails selectionDetails) {
        throw new UnsupportedOperationException("Pickers don't allow context menu.");
    }

    /**
     * @see DirectoryFragment#onCreateContextMenu
     *
     * Called when user tries to generate a context menu anchored to a file when the selection
     * doesn't contain any folder.
     *
     * @param selectionDetails
     *      containsFiles may return false because this may be called when user right clicks on an
     *      unselectable item in pickers
     */
    @VisibleForTesting
    public void updateContextMenuForFiles(Menu menu, SelectionDetails selectionDetails) {
        assert(selectionDetails != null);

        MenuItem share = menu.findItem(R.id.menu_share);
        MenuItem open = menu.findItem(R.id.menu_open);
        MenuItem openWith = menu.findItem(R.id.menu_open_with);
        MenuItem rename = menu.findItem(R.id.menu_rename);

        updateShare(share, selectionDetails);
        updateOpenInContextMenu(open, selectionDetails);
        updateOpenWith(openWith, selectionDetails);
        updateRename(rename, selectionDetails);

        updateContextMenu(menu, selectionDetails);
    }

    /**
     * @see DirectoryFragment#onCreateContextMenu
     *
     * Called when user tries to generate a context menu anchored to a folder when the selection
     * doesn't contain any file.
     *
     * @param selectionDetails
     *      containDirectories may return false because this may be called when user right clicks on
     *      an unselectable item in pickers
     */
    @VisibleForTesting
    public void updateContextMenuForDirs(Menu menu, SelectionDetails selectionDetails) {
        assert(selectionDetails != null);

        MenuItem openInNewWindow = menu.findItem(R.id.menu_open_in_new_window);
        MenuItem rename = menu.findItem(R.id.menu_rename);
        MenuItem pasteInto = menu.findItem(R.id.menu_paste_into_folder);

        updateOpenInNewWindow(openInNewWindow, selectionDetails);
        updateRename(rename, selectionDetails);
        updatePasteInto(pasteInto, selectionDetails);

        updateContextMenu(menu, selectionDetails);
    }

    /**
     * @see DirectoryFragment#onCreateContextMenu
     *
     * Update shared context menu items of both files and folders context menus.
     */
    @VisibleForTesting
    public void updateContextMenu(Menu menu, SelectionDetails selectionDetails) {
        assert(selectionDetails != null);

        MenuItem cut = menu.findItem(R.id.menu_cut_to_clipboard);
        MenuItem copy = menu.findItem(R.id.menu_copy_to_clipboard);
        MenuItem delete = menu.findItem(R.id.menu_delete);

        final boolean canCopy =
                selectionDetails.size() > 0 && !selectionDetails.containsPartialFiles();
        final boolean canDelete = selectionDetails.canDelete();
        cut.setEnabled(canCopy && canDelete);
        copy.setEnabled(canCopy);
        delete.setEnabled(canDelete);
    }

    /**
     * @see DirectoryFragment#onCreateContextMenu
     *
     * Called when user tries to generate a context menu anchored to an empty pane.
     */
    @VisibleForTesting
    public void updateContextMenuForContainer(Menu menu) {
        MenuItem paste = menu.findItem(R.id.menu_paste_from_clipboard);
        MenuItem selectAll = menu.findItem(R.id.menu_select_all);

        paste.setEnabled(mDirDetails.hasItemsToPaste() && mDirDetails.canCreateDoc());
        updateSelectAll(selectAll);
    }

    /**
     * @see RootsFragment#onCreateContextMenu
     */
    public void updateRootContextMenu(Menu menu, RootInfo root, DocumentInfo docInfo) {
        MenuItem eject = menu.findItem(R.id.menu_eject_root);
        MenuItem pasteInto = menu.findItem(R.id.menu_paste_into_folder);
        MenuItem openInNewWindow = menu.findItem(R.id.menu_open_in_new_window);
        MenuItem settings = menu.findItem(R.id.menu_settings);

        updateEject(eject, root);
        updatePasteInto(pasteInto, root, docInfo);
        updateOpenInNewWindow(openInNewWindow, root);
        updateSettings(settings, root);
    }

    protected void updateModePicker(MenuItem grid, MenuItem list) {
        grid.setVisible(mState.derivedMode != State.MODE_GRID);
        list.setVisible(mState.derivedMode != State.MODE_LIST);
    }

    protected void updateAdvanced(MenuItem advanced) {
        advanced.setVisible(mState.showAdvancedOption);
        advanced.setTitle(mState.showAdvancedOption && mState.showAdvanced
                ? R.string.menu_advanced_hide : R.string.menu_advanced_show);
    }

    protected void updateSettings(MenuItem settings) {
        settings.setVisible(false);
    }

    protected void updateSettings(MenuItem settings, RootInfo root) {
        settings.setVisible(false);
    }

    protected void updateEject(MenuItem eject, RootInfo root) {
        eject.setVisible(false);
    }

    protected void updateNewWindow(MenuItem newWindow) {
        newWindow.setVisible(false);
    }

    protected void updateOpenInActionMode(MenuItem open, SelectionDetails selectionDetails) {
        open.setVisible(false);
    }

    protected void updateOpenWith(MenuItem openWith, SelectionDetails selectionDetails) {
        openWith.setVisible(false);
    }

    protected void updateOpenInNewWindow(
            MenuItem openInNewWindow, SelectionDetails selectionDetails) {
        openInNewWindow.setVisible(false);
    }

    protected void updateOpenInNewWindow(
            MenuItem openInNewWindow, RootInfo root) {
        openInNewWindow.setVisible(false);
    }

    protected void updateShare(MenuItem share, SelectionDetails selectionDetails) {
        share.setVisible(false);
    }

    protected void updateDelete(MenuItem delete, SelectionDetails selectionDetails) {
        delete.setVisible(false);
    }

    protected void updateRename(MenuItem rename, SelectionDetails selectionDetails) {
        rename.setVisible(false);
    }

    protected void updateMoveTo(MenuItem moveTo, SelectionDetails selectionDetails) {
        moveTo.setVisible(false);
    }

    protected void updateCopyTo(MenuItem copyTo, SelectionDetails selectionDetails) {
        copyTo.setVisible(false);
    }

    protected void updatePasteInto(MenuItem pasteInto, SelectionDetails selectionDetails) {
        pasteInto.setVisible(false);
    }

    protected void updatePasteInto(MenuItem pasteInto, RootInfo root, DocumentInfo docInfo) {
        pasteInto.setVisible(false);
    }

    protected abstract void updateOpenInContextMenu(
            MenuItem open, SelectionDetails selectionDetails);
    protected abstract void updateSelectAll(MenuItem selectAll);
    protected abstract void updateCreateDir(MenuItem createDir);

    /**
     * Access to meta data about the selection.
     */
    public interface SelectionDetails {
        boolean containsDirectories();

        boolean containsFiles();

        int size();

        boolean containsPartialFiles();

        // TODO: Update these to express characteristics instead of answering concrete questions,
        // since the answer to those questions is (or can be) activity specific.
        boolean canDelete();

        boolean canRename();

        boolean canPasteInto();
    }

    public static class DirectoryDetails {
        private final BaseActivity mActivity;

        public DirectoryDetails(BaseActivity activity) {
            mActivity = activity;
        }

        public boolean shouldShowFancyFeatures() {
            return Shared.shouldShowFancyFeatures(mActivity);
        }

        public boolean hasRootSettings() {
            return mActivity.getCurrentRoot().hasSettings();
        }

        public boolean hasItemsToPaste() {
            return false;
        }

        public boolean canCreateDoc() {
            return isInRecents() ? false : mActivity.getCurrentDirectory().isCreateSupported();
        }

        public boolean isInRecents() {
            return mActivity.getCurrentDirectory() == null;
        }

        public boolean canCreateDirectory() {
            return mActivity.canCreateDirectory();
        }
    }
}
