package dropit.mobile.ui.transfer

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.*
import dropit.mobile.R
import dropit.mobile.ui.RecyclerItemTouchHelper
import dropit.mobile.ui.transfer.adapter.ListFileAdapter
import dropit.mobile.ui.transfer.model.ListFile
import kotlinx.android.synthetic.main.fragment_file_list.*

/**
 *
 */
class FileListFragment : Fragment(), RecyclerItemTouchHelper.SwipeListener {
    lateinit var items: ArrayList<ListFile>
    lateinit var listFileAdapter: ListFileAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_file_list, container, false)
    }

    override fun onAttach(context: Context) {
        setHasOptionsMenu(true)
        super.onAttach(context)
        if (context is SendTransferActivity) {
            items = context.items
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listFileAdapter = ListFileAdapter(context!!, items)
        recyclerView.layoutManager = LinearLayoutManager(context!!.applicationContext)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = listFileAdapter
        val swipeCallback = RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this)
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    override fun onDetach() {
        super.onDetach()
        items = ArrayList()
    }

    override fun onSwiped(viewHolder: ListFileAdapter.ListViewHolder, direction: Int) {
        val index = viewHolder.adapterPosition
        val listFile = items.get(index)
        listFileAdapter.remove(index)
        val snackbar = Snackbar.make(coordinatorLayout, getString(R.string.removed_from_transfer), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.undo), {
            listFileAdapter.add(listFile, index)
        })
        snackbar.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.file_list_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.send -> (context as SendTransferActivity).showServerList()
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(items: ArrayList<ListFile>) =
                FileListFragment().apply {
                    this.items = items
                }
    }
}
