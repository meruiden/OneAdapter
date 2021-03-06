package com.idanatz.oneadapter.tests.module_configs.selection

import android.graphics.Color
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.idanatz.oneadapter.external.interfaces.Item
import com.idanatz.oneadapter.external.modules.ItemModule
import com.idanatz.oneadapter.external.modules.ItemSelectionModule
import com.idanatz.oneadapter.external.modules.ItemSelectionModuleConfig
import com.idanatz.oneadapter.external.states.SelectionState
import com.idanatz.oneadapter.helpers.getViewLocationOnScreen
import com.idanatz.oneadapter.helpers.BaseTest
import com.idanatz.oneadapter.internal.holders.ViewBinder
import com.idanatz.oneadapter.internal.utils.extensions.let2
import com.idanatz.oneadapter.models.TestModel
import com.idanatz.oneadapter.test.R
import org.amshove.kluent.shouldEqualTo
import org.junit.Test
import org.junit.runner.RunWith

private const val IS_SELECTED_COUNT_INDEX = 0
private const val IS_NOT_SELECTED_COUNT_INDEX = 1

@RunWith(AndroidJUnit4::class)
class WhenSelectionTypeSingleSelectingMultipleItemIsNotSupported : BaseTest() {

	private val modelsEvents = hashMapOf<TestModel, IntArray>()

	@Test
	fun test() {
		configure {
			val models = modelGenerator.generateModels(2)
			modelsEvents[models[0]] = IntArray(2)
			modelsEvents[models[1]] = IntArray(2)

			prepareOnActivity {
				oneAdapter
						.attachItemModule(TestItemModule().addState(TestSelectionState()))
						.attachItemSelectionModule(TestItemSelectionModule())
			}
			actOnActivity {
				oneAdapter.add(models)

				runWithDelay {
					val firstHolderRootView = recyclerView.findViewHolderForAdapterPosition(0)?.itemView
					val secondsHolderRootView = recyclerView.findViewHolderForAdapterPosition(1)?.itemView

					let2(firstHolderRootView, secondsHolderRootView) { rootView1: View, rootView2: View ->
						rootView1.post {
							val (x1, y1) = rootView1.getViewLocationOnScreen()
							touchSimulator.simulateLongTap(recyclerView, x1, y1)

							runWithDelay(100) {
								val (x2, y2) = rootView2.getViewLocationOnScreen()
								touchSimulator.simulateTap(recyclerView, x2, y2)
							}
						}
					}
				}
			}
			untilAsserted {
				modelsEvents[models[0]]!![IS_SELECTED_COUNT_INDEX] shouldEqualTo(1)
				modelsEvents[models[0]]!![IS_NOT_SELECTED_COUNT_INDEX] shouldEqualTo(1)
				modelsEvents[models[1]]!![IS_SELECTED_COUNT_INDEX] shouldEqualTo(1)
			}
		}
	}

	inner class TestItemModule : ItemModule<TestModel>() {
		override fun provideModuleConfig() = modulesGenerator.generateValidItemModuleConfig(R.layout.test_model_large)
		override fun onBind(item: Item<TestModel>, viewBinder: ViewBinder) {
			if (item.metadata.isSelected) {
				viewBinder.rootView.setBackgroundColor(Color.parseColor("#C7226E"))
			} else {
				viewBinder.rootView.setBackgroundColor(Color.parseColor("#ffffff"))
			}
		}
	}

	private inner class TestSelectionState : SelectionState<TestModel>() {
        override fun isSelectionEnabled(model: TestModel): Boolean = true
		override fun onSelected(model: TestModel, selected: Boolean) {
			if (selected) {
				modelsEvents[model]?.let {
					it[IS_SELECTED_COUNT_INDEX]++
				}
			} else {
				modelsEvents[model]?.let {
					it[IS_NOT_SELECTED_COUNT_INDEX]++
				}
			}
		}
	}

	private class TestItemSelectionModule : ItemSelectionModule() {
		override fun provideModuleConfig(): ItemSelectionModuleConfig = object : ItemSelectionModuleConfig() {
			override fun withSelectionType() = SelectionType.Single
		}
	}
}