package com.ar.sceneglb

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.utils.Utils
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.RenderableInstance
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.InstructionsController
import com.google.ar.sceneform.ux.TransformableNode
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture

class ArActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private var model: ModelRenderable? = null
    var renderable_Instance:RenderableInstance? = null
    var arSceneformSession: Session? = null
    var arFrame: Frame? = null
    var arSceneformConfig: Config? = null
    var arSceneView: Scene? = null
    var arSceneformScene: Scene? = null
    var arCameraPose: Pose? = null
    private var modelNode:TransformableNode? = null
    private var isTappedOnce:Boolean = false
    private val loaders: Set<CompletableFuture<*>> = HashSet()
    private var anchorNode: AnchorNode? = null
    var hitResult:HitResult? =null

    var url:String = ""
    var trackingState = TrackingState.STOPPED
    val onTouchListener :Scene.OnTouchListener
        get() {
            TODO()
        }
//    var spinner:Spinner? = null
    var btndance:Button? = null
    var btntalk:Button? = null

    @SuppressLint("ResourceType")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)
        url = intent.getStringExtra("URI").toString()
        arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
        arFragment?.instructionsController?.setVisible(
            InstructionsController.TYPE_PLANE_DISCOVERY,
            true
        )
        btndance = findViewById(R.id.dancing)
        btntalk = findViewById(R.id.talking)
//        modelNode = TransformableNode(arFragment?.transformationSystem)
//        spinner = findViewById(R.id.spinner)
//        val arrayAdapter:ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(this,R.array.modes,
//            android.R.layout.simple_spinner_item)
//        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
//        spinner?.adapter = arrayAdapter
//        spinner?.setOnClickListener {
//            it.
//        }
//        Toast.makeText(this,url, Toast.LENGTH_SHORT).show()
//        arFragment?.setOnTapPlaneGlbModel("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb")
//        arFragment?.setOnTapPlaneGlbModel("models/skeleton.glb")
        arFragment?.setOnSessionConfigurationListener { session, config ->
            arSceneformSession = session
            arSceneformConfig = config
            arFragment?.arSceneView?.scene?.camera?.farClipPlane = 750f
            config.focusMode = Config.FocusMode.AUTO
            arFragment?.arSceneView?.scene?.renderer?.addLight(Color.WHITE)
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        }
        arFragment?.setOnViewCreatedListener {
            arSceneformConfig?.depthMode = Config.DepthMode.DISABLED
            arSceneformConfig?.focusMode = Config.FocusMode.AUTO
            arFragment?.arSceneView?.setMaxFramesPerSeconds(60)
            arSceneformScene = arFragment?.arSceneView?.scene
            val node = Node() //Sceneform Node
            arSceneformScene?.addChild(node)
            arSceneformScene?.renderer?.addLight(Color.WHITE)
        }
        arFragment?.arSceneView?.scene?.addOnUpdateListener {
            arSceneformSession = arFragment?.arSceneView?.session
            arSceneView = arFragment?.arSceneView?.scene
            arFrame = arSceneformSession?.update()
            arCameraPose = arFrame?.camera?.displayOrientedPose
            if (trackingState != arSceneformSession?.update()?.camera?.trackingState) {
                trackingState = arSceneformSession?.update()?.camera?.trackingState!!
            }
            Log.d("devhell", "onCreate: ${arFragment?.arSceneView?.scene}")
        }
        arFragment?.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            // Create the Anchor
            this.hitResult = hitResult
            modelNode = TransformableNode(arFragment?.transformationSystem)
            anchorNode = AnchorNode(hitResult.createAnchor())
            Log.d("devhell", "onCreate:${arSceneformScene} ")
            arSceneformScene?.setOnTouchListener { hitTestResult, motionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    val filamentAsset = hitTestResult.node?.renderableInstance?.filamentAsset
                    val renderAsset = hitTestResult.node?.renderableInstance?.renderable
                    val uri = filamentAsset?.entities
                    Log.d("devhell", "renderasset:${renderAsset?.submeshCount} ")
                }
                true
            }
            if (!isTappedOnce) {
                if (model != null) {
                    arSceneformScene?.addChild(anchorNode?.apply {
                        // Create the transformable model and add it to the anchor
                        addChild(modelNode?.apply {
                            renderable = model
                            isTappedOnce = true
                            arFragment?.instructionsController?.setVisible(
                                InstructionsController.TYPE_PLANE_DISCOVERY,
                                false
                            )
                            arFragment?.arSceneView?.planeRenderer?.isEnabled = false
                            renderableInstance.animate(true).start()
                            renderable_Instance = renderableInstance
                            Log.d("amal", "onCreate: $renderable_Instance")
                        })
                        modelNode?.select()
                        modelNode?.localScale = Vector3(5f,5f,5f)
                    } )
                }
                else
                {
                    Toast.makeText(this,"Model not Loaded, tap a button to load one",Toast.LENGTH_SHORT).show()
                }
            }
        }
        loadModels("models/avatar_talking.glb")
        btndance?.setOnClickListener {
            val uri = "models/dancing.glb"
            modelNode?.renderable = null
            anchorNode = null
            modelNode?.select()
            arFragment?.arSceneView?.scene?.renderer?.filamentRenderer?.clearOptions
            isTappedOnce = false
            arFragment?.arSceneView?.planeRenderer?.isEnabled = true
            loadModels2(uri)
        }
        btntalk?.setOnClickListener {
            val uri = "models/avatar_talking.glb"
            modelNode?.renderable = null
            anchorNode = null
            isTappedOnce = false
            model = null
            arFragment?.arSceneView?.planeRenderer?.isEnabled = true
            loadModels2(uri)
        }

//       modelNode.setOnTapListener { hitTestResult, motionEvent ->
//           Log.d("devhell", "onCreate:${modelNode?.renderableInstance?.renderable?.submeshCount} ")
//           Log.d("devhell", "onCreate:${model?.submeshCount} ")
//           Log.d("devhell", "onCreate:${arSceneformScene} ")
//           val rendr = modelNode?.renderableInstance?.renderable?.submeshCount
//
//       }
    }
    private fun loadModel() {
        loaders.plus(ModelRenderable.builder()
            .setSource(this, Uri.parse("models/Talking.glb"))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { model: ModelRenderable ->
                this.model = model
            }
            .exceptionally { throwable: Throwable? ->
                Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG).show()
                null
            })
    }

    private fun loadModels(urin: String) {
        val weakActivity = WeakReference(this)
        Log.d("devhell", "loadModels:$urin")
        //Loading 3D Model:
        ModelRenderable.builder()
            .setSource(this, Uri.parse(urin))
            .setIsFilamentGltf(true)
            .setAsyncLoadEnabled(true)
            .build()
            .thenAccept { model: ModelRenderable? ->
                val activity = weakActivity.get()
                if (activity != null) {
                    activity.model = model
                    modelNode?.renderable = activity.model
                }
            }
            .exceptionally { throwable: Throwable? ->
                Toast.makeText(
                    this, "Unable to load model-> $throwable", Toast.LENGTH_LONG
                ).show()
                null
            }
    }
    private fun loadModels2(urin: String) {
        modelNode?.renderable = null
        val weakActivity = WeakReference(this)
        Log.d("devhell", "loadModels:$urin")
        //Loading 3D Model:
        ModelRenderable.builder()
            .setSource(this, Uri.parse(urin))
            .setIsFilamentGltf(true)
            .setAsyncLoadEnabled(true)
            .build()
            .thenAccept { model: ModelRenderable? ->
                val activity = weakActivity.get()
                if (activity != null) {
                    activity.model = model
                    modelNode = TransformableNode(arFragment?.transformationSystem)
                    anchorNode = AnchorNode(hitResult?.createAnchor())
                    Log.d("devhell", "onCreate:${arSceneformScene} ")
                    if (model != null) {
                        arSceneformScene?.addChild(anchorNode?.apply {
                            // Create the transformable model and add it to the anchor
                            addChild(modelNode?.apply {
                                renderable = model
                                isTappedOnce = true
                                arFragment?.instructionsController?.setVisible(
                                    InstructionsController.TYPE_PLANE_DISCOVERY,
                                    false
                                )
                                arFragment?.arSceneView?.planeRenderer?.isEnabled = false
                                renderableInstance.animate(true).start()
                            })
                            modelNode?.select()
                            modelNode?.localScale = Vector3(5f,5f,5f)
                        } )
                    }
                    else
                    {
                        Toast.makeText(this,"Model not Loaded, tap a button to load one",Toast.LENGTH_SHORT).show()
                    }

                }
            }
            .exceptionally { throwable: Throwable? ->
                Toast.makeText(
                    this, "Unable to load model-> $throwable", Toast.LENGTH_LONG
                ).show()
                null
            }
    }
}