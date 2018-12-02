package dropit.mobile.ui.transfer

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dropit.mobile.R
import dropit.mobile.ui.transfer.adapter.ListServerAdapter
import dropit.mobile.ui.transfer.model.ListServer
import kotlinx.android.synthetic.main.fragment_file_list.*
import java.util.*


/**
 *
 */
class ServerListFragment : Fragment() {
    var items = ArrayList<ListServer>()
    lateinit var listServerAdapter: ListServerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items.add(ListServer("Teste", UUID.randomUUID(), 80, "192.168.100.1"))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_server_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listServerAdapter = ListServerAdapter(items)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = listServerAdapter
        listServerAdapter.notifyDataSetChanged()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }
}
