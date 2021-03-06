package com.grimmauld.createintegration.misc;

import com.grimmauld.createintegration.CreateIntegration;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import net.minecraft.util.math.BlockPos;

//import java.util.HashMap;

public class ChunkLoaderMovementBehaviour extends MovementBehaviour {

    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        if (context.world.isRemote)return;

        iVec2d chunknew=new iVec2d(pos);
        chunknew.x>>=4;chunknew.y>>=4;//mact position zu einem chunk

        if (!(context.temporaryData instanceof iVec2d)){
            if(context.data.contains("chunknew")) {
                context.temporaryData = new iVec2d(context.data.getLong("chunknew"));
            }else{
                context.temporaryData=chunknew;
                //CreateIntegration.logger.debug("wtf just happend");//visitNewPosition wurde vor startMoving aufgerufen
                writeExtraData(context);
                forceload(context,chunknew,true);
            }
        }
        iVec2d chunkalt= (iVec2d) context.temporaryData;

        if(!chunknew.equals(chunkalt)){
            forceload(context,chunkalt,false);
            forceload(context,chunknew,true);
            context.temporaryData=chunknew;
        }
    }

    //private void updatepos(){ }



    @Override
    public void startMoving(MovementContext context){

        //CreateIntegration.logger.debug("start");
        //CreateIntegration.logger.debug(context.position); probably null
        /*if(context.position!=null){
            iVec2d chunkstart= new iVec2d((int)(context.position.x),(int)(context.position.z));
            chunkstart.x>>=4;chunkstart.y>>=4;
            context.temporaryData=chunkstart;
            forceload(context,chunkstart,true);
        }*/

        //CreateIntegration.logger.debug("start");
        //chunk.put(context,null);
    }

    @Override
    public void stopMoving(MovementContext context){
        iVec2d chunkalt= (iVec2d) context.temporaryData;
        forceload(context,chunkalt,false);
        //CreateIntegration.logger.debug("stop");

    }

    @Override
    public void writeExtraData(MovementContext context) {
        super.writeExtraData(context);
        if(context.temporaryData instanceof iVec2d) {
            iVec2d chunkalt = (iVec2d) context.temporaryData;
            context.data.putLong("chunknew", chunkalt.toLong());
            CreateIntegration.logger.debug("minecatr saved");
        }else{
            CreateIntegration.logger.debug("i don't want to write null");
        }
    }

    //loads/unloads the specified chunk
    private void forceload(MovementContext context,iVec2d chunk,boolean state){
        if(state){
            context.world.getCapability(CreateIntegration.CHUNK_LOADING_CAPABILITY, null).ifPresent(cap -> cap.addchunk(chunk));
        }else{
            context.world.getCapability(CreateIntegration.CHUNK_LOADING_CAPABILITY, null).ifPresent(cap -> cap.removechunk(chunk));
        }
    }
}
