package mindustry.world.blocks.logic;

import arc.*;
import arc.Input.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import arc.util.pooling.*;
import mindustry.gen.*;
import mindustry.net.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MessageBlock extends Block{
    //don't change this too much unless you want to run into issues with packet sizes
    public int maxTextLength = 220;
    public int maxNewlines = 24;

    public MessageBlock(String name){
        super(name);
        configurable = true;
        solid = true;
        destructible = true;
        entityType = MessageBlockEntity::new;

        config(String.class, (tile, text) -> {
            if(net.server() && text.length() > maxTextLength){
                throw new ValidateException(player, "Player has gone above text limit.");
            }

            MessageBlockEntity entity = tile.ent();

            StringBuilder result = new StringBuilder(text.length());
            text = text.trim();
            int count = 0;
            for(int i = 0; i < text.length(); i++){
                char c = text.charAt(i);
                if(c == '\n' || c == '\r'){
                    count ++;
                    if(count <= maxNewlines){
                        result.append('\n');
                    }
                }else{
                    result.append(c);
                }
            }

            entity.message = result.toString();
            entity.lines = entity.message.split("\n");
        });
    }

    @Override
    public void drawSelect(Tile tile){
        MessageBlockEntity entity = tile.ent();
        BitmapFont font = Fonts.outline;
        GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.getData().setScale(1 / 4f / Scl.scl(1f));
        font.setUseIntegerPositions(false);

        String text = entity.message == null || entity.message.isEmpty() ? "[lightgray]" + Core.bundle.get("empty") : entity.message;

        l.setText(font, text, Color.white, 90f, Align.left, true);
        float offset = 1f;

        Draw.color(0f, 0f, 0f, 0.2f);
        Fill.rect(tile.drawx(), tile.drawy() - tilesize/2f - l.height/2f - offset, l.width + offset*2f, l.height + offset*2f);
        Draw.color();
        font.setColor(Color.white);
        font.draw(text, tile.drawx() - l.width/2f, tile.drawy() - tilesize/2f - offset, 90f, Align.left, true);
        font.setUseIntegerPositions(ints);

        font.getData().setScale(1f);

        Pools.free(l);
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        MessageBlockEntity entity = tile.ent();

        table.addImageButton(Icon.pencil, () -> {
            if(mobile){
                Core.input.getTextInput(new TextInput(){{
                    text = entity.message;
                    multiline = true;
                    maxLength = maxTextLength;
                    accepted = tile::configure;
                }});
            }else{
                FloatingDialog dialog = new FloatingDialog("$editmessage");
                dialog.setFillParent(false);
                TextArea a = dialog.cont.add(new TextArea(entity.message.replace("\n", "\r"))).size(380f, 160f).get();
                a.setFilter((textField, c) -> {
                    if(c == '\n' || c == '\r'){
                        int count = 0;
                        for(int i = 0; i < textField.getText().length(); i++){
                            if(textField.getText().charAt(i) == '\n' || textField.getText().charAt(i) == '\r'){
                                count++;
                            }
                        }
                        return count < maxNewlines;
                    }
                    return true;
                });
                a.setMaxLength(maxTextLength);
                dialog.buttons.addButton("$ok", () -> {
                    tile.configure(a.getText());
                    dialog.hide();
                }).size(130f, 60f);
                dialog.update(() -> {
                    if(tile.block() != this){
                        dialog.hide();
                    }
                });
                dialog.show();
            }
            control.input.frag.config.hideConfig();
        }).size(40f);
    }

    @Override
    public void updateTableAlign(Tile tile, Table table){
        Vec2 pos = Core.input.mouseScreen(tile.drawx(), tile.drawy() + tile.block().size * tilesize / 2f + 1);
        table.setPosition(pos.x, pos.y, Align.bottom);
    }

    public class MessageBlockEntity extends TileEntity{
        public String message = "";
        public String[] lines = {""};

        @Override
        public String config(){
            return message;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.str(message);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            message = read.str();
        }
    }
}
