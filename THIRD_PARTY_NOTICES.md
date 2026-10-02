# Third-party code in Regnum

## LLibrary ModelAnimator

`client/render/AdaptedModelAnimator.java` adapts **iLexiconn's LLibrary ModelAnimator**, licensed under GNU LGPL version 2.1.

Original source: https://github.com/iLexiconn/LLibrary/blob/1.11.2/src/main/java/net/ilexiconn/llibrary/client/model/ModelAnimator.java

Original source and license are retained in `_refs/llibrary-source/`; the license is also in `coord/LLibrary-LICENSE.md`. The adapted file remains under LGPL-2.1. This notice does not relicense unrelated Regnum code.

Changes: Minecraft 1.21.1 `ModelPart` API; external synchronized fractional animation tick; preallocated indexed transform buffers; no runtime LLibrary/Forge dependency. Keyframe transition, hold, reset and sine interpolation follow the upstream implementation. Regnum's creature poses are authored for its own skeletons.

The adapted animator also drives Regnum-authored soldier shot/recoil clips. Their six-tick timeline follows the server swing signal; it does not invent a server reload phase.
