package com.ss.editor.tree.generator.tree.factory;

import static com.ss.rlib.common.util.ClassUtils.unsafeCast;
import com.simsilica.arboreal.BranchParameters;
import com.simsilica.arboreal.LevelOfDetailParameters;
import com.simsilica.arboreal.TreeParameters;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.tree.generator.parameters.*;
import com.ss.editor.tree.generator.tree.node.*;
import com.ss.editor.ui.control.tree.node.TreeNode;
import com.ss.editor.ui.control.tree.node.factory.TreeNodeFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of node factory to create parameters nodes.
 *
 * @author JavaSaBr
 */
public class ParametersTreeNodeFactory implements TreeNodeFactory {

    private static final TreeNodeFactory INSTANCE = new ParametersTreeNodeFactory();

    @FromAnyThread
    public static @NotNull TreeNodeFactory getInstance() {
        return INSTANCE;
    }

    @Override
    @FxThread
    public <T, V extends TreeNode<T>> @Nullable V createFor(@Nullable T element, long objectId) {

        if (element instanceof ProjectParameters) {
            return unsafeCast(new ProjectParametersTreeNode((ProjectParameters) element, objectId));
        } else if (element instanceof MaterialsParameters) {
            return unsafeCast(new MaterialsParametersTreeNode((MaterialsParameters) element, objectId));
        } else if (element instanceof TreeParameters) {
            return unsafeCast(new TreeParametersTreeNode((TreeParameters) element, objectId));
        } else if (element instanceof LodsParameters) {
            return unsafeCast(new LodsParametersTreeNode((LodsParameters) element, objectId));
        } else if (element instanceof RootsParameters) {
            return unsafeCast(new RootsParametersTreeNode((RootsParameters) element, objectId));
        } else if (element instanceof BranchesParameters) {
            return unsafeCast(new BranchesParametersTreeNode((BranchesParameters) element, objectId));
        } else if (element instanceof LevelOfDetailParameters) {
            return unsafeCast(new LodParametersTreeNode((LevelOfDetailParameters) element, objectId));
        } else if (element instanceof BranchParameters) {
            return unsafeCast(new BranchParametersTreeNode((BranchParameters) element, objectId));
        } else if (element instanceof MaterialParameters) {
            return unsafeCast(new MaterialParametersTreeNode((MaterialParameters) element, objectId));
        }

        return null;
    }
}
