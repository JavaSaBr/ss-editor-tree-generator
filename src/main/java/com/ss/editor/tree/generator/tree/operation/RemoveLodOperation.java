package com.ss.editor.tree.generator.tree.operation;

import com.simsilica.arboreal.LevelOfDetailParameters;
import com.simsilica.arboreal.TreeParameters;
import com.ss.editor.annotation.FXThread;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.model.undo.impl.AbstractEditorOperation;
import com.ss.editor.tree.generator.parameters.LodsParameters;
import org.jetbrains.annotations.NotNull;

/**
 * The operation to remove a level of details
 *
 * @author JavaSaBr
 */
public class RemoveLodOperation extends AbstractEditorOperation<ChangeConsumer> {

    /**
     * The tree parameters.
     */
    @NotNull
    private final TreeParameters treeParameters;

    /**
     * The lods parameters.
     */
    @NotNull
    private final LodsParameters lodsParameters;

    /**
     * The level of details to remove.
     */
    @NotNull
    private final LevelOfDetailParameters removed;

    /**
     * The prev index of the branch.
     */
    private int index;

    public RemoveLodOperation(@NotNull final TreeParameters treeParameters,
                              @NotNull final LodsParameters lodsParameters,
                              @NotNull final LevelOfDetailParameters removed) {
        this.treeParameters = treeParameters;
        this.lodsParameters = lodsParameters;
        this.removed = removed;
    }

    @Override
    @FXThread
    protected void redoImpl(@NotNull final ChangeConsumer editor) {
        EXECUTOR_MANAGER.addJMETask(() -> {
            index = treeParameters.removeLodLevel(removed);
            EXECUTOR_MANAGER.addFXTask(() -> editor.notifyFXRemovedChild(lodsParameters, removed));
        });
    }

    @Override
    @FXThread
    protected void undoImpl(@NotNull final ChangeConsumer editor) {
        EXECUTOR_MANAGER.addJMETask(() -> {
            treeParameters.addLodLevel(removed, index);
            EXECUTOR_MANAGER.addFXTask(() -> editor.notifyFXAddedChild(lodsParameters, removed, index, false));
        });
    }
}
