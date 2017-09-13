package com.ss.editor.tree.generator.parameters;

import com.simsilica.arboreal.Parameters;
import com.simsilica.arboreal.TreeParameters;
import com.ss.editor.annotation.JMEThread;
import org.jetbrains.annotations.NotNull;

/**
 * The implementation of parameters to configure level of details of a tree.
 *
 * @author JavaSaBr
 */
public class LodsParameters extends Parameters {

    /**
     * The tree parameters.
     */
    @NotNull
    private final TreeParameters treeParameters;

    public LodsParameters(@NotNull final TreeParameters treeParameters) {
        this.treeParameters = treeParameters;
    }

    /**
     * @return the tree parameters.
     */
    @JMEThread
    public @NotNull TreeParameters getTreeParameters() {
        return treeParameters;
    }
}
