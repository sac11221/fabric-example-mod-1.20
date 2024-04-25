package com.realmssearch.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.realms.gui.screen.RealmsScreen;
import net.minecraft.client.realms.gui.screen.RealmsSelectWorldTemplateScreen;
import net.minecraft.client.realms.dto.WorldTemplate;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(value = RealmsSelectWorldTemplateScreen.class)
public abstract class RealmsSearchClientMixin extends RealmsScreen {

    @Shadow RealmsSelectWorldTemplateScreen.WorldTemplateObjectSelectionList templateList;
    private TextFieldWidget searchBar;
    private List<WorldTemplate> templateListBase;
    private final Text fetchingText = Text.literal("Fetching all the maps...");
    private boolean isFetching;
    private int mapsLoaded;
    private int changed;
    private String currentSearch;

    protected RealmsSearchClientMixin(Text text) {
        super(text);
    }

    private void sieve(String search){
        /* empty search box, display all elements */
        if (search.equals("") || search.equals("@")) {
            /* small optimization, doesn't reload all elements if they are all already present */
            if (templateList.getValues().size() == templateListBase.size()) return;

            templateList.clear();
            Iterator<WorldTemplate> itr = templateListBase.iterator();
            while (itr.hasNext()) {
                templateList.addEntry(itr.next());
            }
            return;
        }

        templateList.clear();
        Iterator<WorldTemplate> itr = templateListBase.iterator();
        if (search.charAt(0) != '@') {
            while (itr.hasNext()) { /* search by title */
                WorldTemplate temp = itr.next();
                if (temp.name.toLowerCase().contains(search.toLowerCase())) {
                    templateList.addEntry(temp);
                }
            }
        } else {
            while (itr.hasNext()) { /* search by author */
                WorldTemplate temp = itr.next();
                if (temp.author.toLowerCase().contains(search.toLowerCase().substring(1))) {
                    templateList.addEntry(temp);
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "init()V")
    public void init(CallbackInfo info) {
        this.searchBar = new TextFieldWidget(this.textRenderer, this.width/2-75, 5, 150, 20, Text.translatable("selectWorld.search"));
        this.searchBar.setMaxLength(50);
        this.searchBar.setDrawsBackground(true);
        this.searchBar.setVisible(false);
        this.searchBar.setEditableColor(0xFFFFFF);
        this.searchBar.setSuggestion("Search for a map here!");
        this.addSelectableChild(this.searchBar);
        this.searchBar.setChangedListener(
                s -> {
                    this.sieve(s);
                    this.currentSearch = s;
                }
        );
        this.mapsLoaded = 0;
        this.changed = 0;
        this.isFetching = true;
        this.currentSearch = "";
    }

    public void tick() {
        /* add suggestion text */
        if (this.searchBar.isFocused() || (!this.searchBar.isSelected() && !this.currentSearch.equals(""))) {
            this.searchBar.setSuggestion(null);
        } else {
            this.searchBar.setSuggestion("Search for a map here!");
        }

        /* prevent to write before all maps are loaded in, 40 is just an arbitrary number
           and I think that's enough time for most machines... plus very unlikely for players
           to immediately start a search, so it should be fine :p */
        int temp = this.templateList.getValues().size();
        if (temp > this.mapsLoaded) {
            this.mapsLoaded = temp;
            this.changed = 0;
        } else {
            this.changed++;
        }
        if (changed < 40) {
            this.searchBar.setEditable(false);
        } else if (changed == 40) { // we should have gotten all maps at this point
            this.templateListBase = this.templateList.getValues();
            this.searchBar.setVisible(true);
            this.isFetching = false;
        } else {
            this.searchBar.setEditable(true);
        }
    }

    @Inject(at = @At("TAIL"), method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V")
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        this.searchBar.render(context, mouseX, mouseY, delta);
        if (isFetching) {
            context.drawCenteredTextWithShadow(this.textRenderer, fetchingText, this.width / 2, 13, 16777215);
        }
    }
}