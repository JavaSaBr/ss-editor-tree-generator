package com.ss.editor.tree.generator.editor;

import static com.ss.rlib.common.util.ObjectUtils.notNull;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.simsilica.arboreal.BranchParameters;
import com.simsilica.arboreal.LevelOfDetailParameters;
import com.ss.editor.FileExtensions;
import com.ss.editor.Messages;
import com.ss.editor.annotation.BackgroundThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.plugin.api.editor.Advanced3DFileEditorWithSplitRightTool;
import com.ss.editor.tree.generator.PluginMessages;
import com.ss.editor.tree.generator.TreeGeneratorEditorPlugin;
import com.ss.editor.tree.generator.parameters.MaterialParameters;
import com.ss.editor.tree.generator.parameters.ProjectParameters;
import com.ss.editor.tree.generator.property.ParametersPropertyEditor;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.component.asset.tree.context.menu.action.DeleteFileAction;
import com.ss.editor.ui.component.asset.tree.context.menu.action.NewFileAction;
import com.ss.editor.ui.component.asset.tree.context.menu.action.RenameFileAction;
import com.ss.editor.ui.component.editor.EditorDescription;
import com.ss.editor.ui.component.editor.state.EditorState;
import com.ss.editor.ui.component.tab.EditorToolComponent;
import com.ss.editor.ui.control.property.PropertyEditor;
import com.ss.editor.ui.control.tree.NodeTree;
import com.ss.editor.ui.control.tree.node.TreeNode;
import com.ss.editor.ui.css.CssClasses;
import com.ss.editor.ui.util.DynamicIconSupport;
import com.ss.editor.ui.util.UiUtils;
import com.ss.editor.util.EditorUtil;
import com.ss.rlib.common.util.array.Array;
import com.ss.rlib.fx.util.FxUtils;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The implementation of an editor to configure and generate Trees.
 *
 * @author JavaSaBr
 */
public class TreeGeneratorFileEditor extends
        Advanced3DFileEditorWithSplitRightTool<TreeGeneratorEditor3DPart, TreeGeneratorEditorState> implements
        ChangeConsumer {

    private static final Predicate<Class<?>> ACTION_TESTER = type -> type == NewFileAction.class ||
            type == DeleteFileAction.class || type == RenameFileAction.class;

    public static final EditorDescription DESCRIPTION = new EditorDescription();

    static {
        DESCRIPTION.setConstructor(TreeGeneratorFileEditor::new);
        DESCRIPTION.setEditorName(PluginMessages.TREE_GENERATOR_EDITOR_NAME);
        DESCRIPTION.setEditorId(TreeGeneratorFileEditor.class.getSimpleName());
        DESCRIPTION.addExtension(TreeGeneratorEditorPlugin.PROJECT_EXTENSION);
    }

    /**
     * The light toggle.
     */
    @Nullable
    private ToggleButton lightButton;

    /**
     * The parameters tree.
     */
    @Nullable
    private NodeTree<ChangeConsumer> parametersTree;

    /**
     * The project parameters.
     */
    @Nullable
    private ProjectParameters parameters;

    /**
     * The property editor.
     */
    @Nullable
    private PropertyEditor<ChangeConsumer> propertyEditor;

    /**
     * The selection handler.
     */
    @Nullable
    private Consumer<Array<Object>> selectionHandler;

    @Override
    @FxThread
    protected @NotNull TreeGeneratorEditor3DPart create3DEditorPart() {
        return new TreeGeneratorEditor3DPart(this);
    }

    @Override
    protected @Nullable Supplier<EditorState> getEditorStateFactory() {
        return TreeGeneratorEditorState::new;
    }

    @Override
    @FromAnyThread
    public @NotNull EditorDescription getDescription() {
        return DESCRIPTION;
    }

    @Override
    @FxThread
    protected void createToolComponents(@NotNull EditorToolComponent container, @NotNull StackPane root) {
        super.createToolComponents(container, root);

        selectionHandler = this::selectFromTree;
        parametersTree = new NodeTree<>(selectionHandler, this);
        propertyEditor = new ParametersPropertyEditor(this);
        propertyEditor.prefHeightProperty().bind(root.heightProperty());

        container.addComponent(buildSplitComponent(parametersTree, propertyEditor, root),
                PluginMessages.TREE_GENERATOR_EDITOR_TREE_TOOL);

        FxUtils.addClass(parametersTree.getTreeView(), CssClasses.TRANSPARENT_TREE_VIEW);
    }

    /**
     * Handle selected objects from node tree.
     *
     * @param objects the selected objects.
     */
    @FxThread
    private void selectFromTree(@Nullable Array<Object> objects) {

        var object = objects.first();

        Object parent = null;
        Object element;

        if (object instanceof TreeNode<?>) {
            var treeNode = (TreeNode) object;
            var parentNode = treeNode.getParent();
            parent = parentNode == null ? null : parentNode.getElement();
            element = treeNode.getElement();
        } else {
            element = object;
        }

        getPropertyEditor().buildFor(element, parent);
    }

    /**
     * Get the property editor.
     *
     * @return the property editor.
     */
    @FxThread
    private @NotNull PropertyEditor<ChangeConsumer> getPropertyEditor() {
        return notNull(propertyEditor);
    }

    @Override
    @FxThread
    protected void doOpenFile(@NotNull Path file) throws IOException {
        super.doOpenFile(file);

        var importer = BinaryImporter.getInstance();
        importer.setAssetManager(EditorUtil.getAssetManager());

        try (var in = Files.newInputStream(file)) {
            parameters = (ProjectParameters) importer.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        getParametersTree().fill(parameters);
        getEditor3DPart().open(parameters);
    }

    @Override
    @BackgroundThread
    protected void doSave(@NotNull Path toStore) throws IOException {
        super.doSave(toStore);

        var exporter = BinaryExporter.getInstance();

        try (var out = Files.newOutputStream(toStore)) {
            exporter.save(parameters, out);
        }
    }

    @Override
    @FxThread
    protected boolean needToolbar() {
        return true;
    }

    @Override
    @FxThread
    protected void createToolbar(@NotNull HBox container) {
        super.createToolbar(container);

        var exportAction = new Button();
        exportAction.setTooltip(new Tooltip(PluginMessages.TREE_GENERATOR_EDITOR_EXPORT_ACTION));
        exportAction.setOnAction(event -> export());
        exportAction.setGraphic(new ImageView(Icons.EXPORT_16));

        lightButton = new ToggleButton();
        lightButton.setTooltip(new Tooltip(Messages.SCENE_FILE_EDITOR_ACTION_CAMERA_LIGHT));
        lightButton.setGraphic(new ImageView(Icons.LIGHT_16));
        lightButton.setSelected(true);
        lightButton.selectedProperty()
                .addListener((observable, oldValue, newValue) -> changeLight(newValue));

        FxUtils.addClass(exportAction, CssClasses.FLAT_BUTTON, CssClasses.FILE_EDITOR_TOOLBAR_BUTTON)
                .addClass(lightButton, CssClasses.FILE_EDITOR_TOOLBAR_BUTTON);

        DynamicIconSupport.addSupport(exportAction, lightButton);

        FxUtils.addChild(container, createSaveAction(), exportAction, lightButton);
    }

    /**
     * Open Save As Dialog to export the tree.
     */
    @FxThread
    private void export() {
        UiUtils.openSaveAsDialog(this::export, FileExtensions.JME_OBJECT, ACTION_TESTER);
    }

    /**
     * Export the tree to the file.
     *
     * @param path the file.
     */
    @FxThread
    private void export(@NotNull Path path) {

        UiUtils.incrementLoading();

        var editor3DPart = getEditor3DPart();
        editor3DPart.generate(node -> {

            var exporter = BinaryExporter.getInstance();

            try (var out = Files.newOutputStream(path)) {
                exporter.save(node, out);
            } catch (final IOException e) {
                EditorUtil.handleException(LOGGER, this, e);
            }

            UiUtils.decrementLoading();
        });
    }

    /**
     * Handle changing camera light visibility.
     */
    @FxThread
    private void changeLight(@NotNull Boolean newValue) {

        if (isIgnoreListeners()) {
            return;
        }

        var editor3DState = getEditor3DPart();
        editor3DState.updateLightEnabled(newValue);

        if (editorState != null) {
            editorState.setEnableLight(newValue);
        }
    }

    /**
     * Get the parameters tree.
     *
     * @return the parameters tree.
     */
    @FxThread
    private @NotNull NodeTree<ChangeConsumer> getParametersTree() {
        return notNull(parametersTree);
    }

    /**
     * Get the project parameters.
     *
     * @return the project parameters.
     */
    @FxThread
    private @NotNull ProjectParameters getParameters() {
        return notNull(parameters);
    }

    /**
     * Get the light toggle.
     *
     * @return the light toggle.
     */
    @FxThread
    private @NotNull ToggleButton getLightButton() {
        return notNull(lightButton);
    }

    @Override
    @FxThread
    protected void loadState() {
        super.loadState();

        var editorState = getEditorState();
        if (editorState != null) {
            getLightButton().setSelected(editorState.isEnableLight());
        }
    }

    @Override
    @FxThread
    public void notifyFxChangeProperty(@NotNull Object object, @NotNull String propertyName) {

        if (object instanceof MaterialParameters) {
            getParametersTree().refresh(object);
        }

        getPropertyEditor().syncFor(object);
        getEditor3DPart().generate();
    }

    @Override
    @FxThread
    public void notifyFxAddedChild(@NotNull Object parent, @NotNull Object added, int index, boolean needSelect) {

        var parametersTree = getParametersTree();

        if (added instanceof BranchParameters || added instanceof LevelOfDetailParameters) {
            parametersTree.refresh(parent);
        } else {
            parametersTree.notifyAdded(parent, added, index);
        }

        if (needSelect) {
            parametersTree.selectSingle(added);
        }

        getEditor3DPart().generate();
    }

    @Override
    @FxThread
    public void notifyFxRemovedChild(@NotNull Object parent, @NotNull Object removed) {

        if (removed instanceof BranchParameters || removed instanceof LevelOfDetailParameters) {
            getParametersTree().refresh(parent);
        } else {
            getParametersTree().notifyRemoved(parent, removed);
        }

        getEditor3DPart().generate();
    }
}
