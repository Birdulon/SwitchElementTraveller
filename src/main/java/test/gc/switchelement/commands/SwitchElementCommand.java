package test.gc.switchelement.commands;

import emu.grasscutter.GameConstants;
import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ElementType;
import emu.grasscutter.server.packet.send.PacketSceneEntityAppearNotify;

import java.util.List;

@Command(label = "switchelement", usage = "<White|Anemo|Geo|Electro|Dendro>", aliases = {"se"}, threading = true)
public class SwitchElementCommand implements CommandHandler {
    private ElementType getElementFromString(String elementString) {
        return switch (elementString.toLowerCase()) {
            case "white", "common" -> ElementType.None;
            case "fire", "pyro" -> ElementType.Fire;
            case "water", "hydro" -> ElementType.Water;
            case "wind", "anemo", "air" -> ElementType.Wind;
            case "ice", "cryo" -> ElementType.Ice;
            case "rock", "geo" -> ElementType.Rock;
            case "electro" -> ElementType.Electric;
            case "grass", "dendro", "plant" -> ElementType.Grass;
            default -> null;
        };
    }

    private boolean changeAvatarElement(Player player, int avatarId, ElementType element) {
        var avatar = player.getAvatars().getAvatarById(avatarId);
        int depotId = element.getDepotValue() + ((avatarId == GameConstants.MAIN_CHARACTER_MALE) ? 500 : 700);
        var skillDepot = GameData.getAvatarSkillDepotDataMap().get(depotId);
        if (avatar == null || skillDepot == null) {
            return false;
        }
        avatar.setSkillDepotData(skillDepot);
        avatar.setCurrentEnergy(1000);
        avatar.save();
        return true;
    }

    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        if (args.size() != 1) {
            sendUsageMessage(sender);
            return;
        }

        var element = getElementFromString(args.get(0));
        if (element == null) {
            CommandHandler.sendMessage(sender, "Invalid element");
            return;
        }

        boolean maleSuccess = changeAvatarElement(targetPlayer, GameConstants.MAIN_CHARACTER_MALE, element);
        boolean femaleSuccess = changeAvatarElement(targetPlayer, GameConstants.MAIN_CHARACTER_FEMALE, element);
        if (maleSuccess || femaleSuccess) {
            int scene = targetPlayer.getSceneId();
            var senderPos = targetPlayer.getPosition();
            targetPlayer.getWorld().transferPlayerToScene(targetPlayer, 1, senderPos);
            targetPlayer.getWorld().transferPlayerToScene(targetPlayer, scene, senderPos);
            targetPlayer.getScene().broadcastPacket(new PacketSceneEntityAppearNotify(targetPlayer));
            CommandHandler.sendMessage(sender, "Successfully changed traveller to " + element.name());
        } else {
            CommandHandler.sendMessage(sender, "Failed to change the Element.");
        }
    }
}