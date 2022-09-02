package test.gc.switchelement.commands;

import emu.grasscutter.GameConstants;
import emu.grasscutter.command.Command;
import emu.grasscutter.command.CommandHandler;
import emu.grasscutter.data.GameData;
import emu.grasscutter.game.player.Player;
import emu.grasscutter.game.props.ElementType;
import emu.grasscutter.server.packet.send.PacketSceneEntityAppearNotify;

import java.util.List;

@Command(label = "switchelement", usage = "<White|Anemo|Geo|Electro|Dendro>", aliases = {"se", "depot"}, threading = true)
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

    @Override
    public void execute(Player sender, Player targetPlayer, List<String> args) {
        if (args.size() != 1) {
            sendUsageMessage(sender);
            return;
        }

        var avatar = targetPlayer.getTeamManager().getCurrentAvatarEntity().getAvatar();  // potential NPEs but w/e
        if (avatar == null) return;  // should never happen but w/e

        int avatarId = avatar.getAvatarId();
        int depotId;

        try {
            depotId = Integer.parseInt(args.get(0));
        } catch (NumberFormatException ignored) {
            var element = getElementFromString(args.get(0));
            if (element == null) {
                CommandHandler.sendMessage(sender, "Invalid element.");
                return;
            }
            depotId = element.getDepotValue() + ((avatarId == GameConstants.MAIN_CHARACTER_FEMALE) ? 700 : 500);
        }

        var skillDepot = GameData.getAvatarSkillDepotDataMap().get(depotId);
        if (skillDepot == null) {
            CommandHandler.sendMessage(sender, "Invalid skill depot.");
            return;
        }

        avatar.setSkillDepotData(skillDepot);
        avatar.setCurrentEnergy(1000);
        avatar.save();
        CommandHandler.sendMessage(sender, "Successfully changed depot to " + depotId);

        // Reload scene to apply changes
        int scene = targetPlayer.getSceneId();
        var senderPos = targetPlayer.getPosition();
        targetPlayer.getWorld().transferPlayerToScene(targetPlayer, 1, senderPos);
        targetPlayer.getWorld().transferPlayerToScene(targetPlayer, scene, senderPos);
        targetPlayer.getScene().broadcastPacket(new PacketSceneEntityAppearNotify(targetPlayer));
    }
}