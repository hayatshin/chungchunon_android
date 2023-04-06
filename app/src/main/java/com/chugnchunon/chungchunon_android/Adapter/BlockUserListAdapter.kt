package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.BlockUserListActivity
import com.chugnchunon.chungchunon_android.DataClass.BlockUser
import com.chugnchunon.chungchunon_android.DataClass.LikePerson
import com.chugnchunon.chungchunon_android.EnlargeAvatarActivity
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.block_user_line.view.*
import kotlinx.android.synthetic.main.like_person_line.view.*
import kotlinx.android.synthetic.main.like_person_line.view.likePersonAvatar

class BlockUserListAdapter(
    val context: Context,
    private val likePersonList: ArrayList<BlockUser>
) : RecyclerView.Adapter<BlockUserListAdapter.ViewHolder>() {

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var diaryDB = Firebase.firestore.collection("diary")

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val likePersonAvatar = view?.findViewById<ImageView>(R.id.likePersonAvatar)
        val likePersonName = view?.findViewById<TextView>(R.id.likePersonName)
        val likePersonRegion = view?.findViewById<TextView>(R.id.likePersonRegion)

        fun bind(context: Context, position: Int) {
            Glide.with(context)
                .load(likePersonList[position].userAvatar)
                .into(likePersonAvatar!!)

            likePersonName!!.text = likePersonList[position].userName
            likePersonRegion!!.text = likePersonList[position].userRegion
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BlockUserListAdapter.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.block_user_line, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlockUserListAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)

        holder.itemView.cancelBlock.setOnClickListener { view ->

            val blockUser = likePersonList[position].userId

            userDB.document("${userId}")
                .update("blockUserList", (FieldValue.arrayRemove("$blockUser")))
                .addOnSuccessListener {
                    // 다이어리에 차단 해제
                    diaryDB.whereEqualTo("userId", blockUser).get()
                        .addOnSuccessListener { documents ->
                            documents.forEachIndexed { index, queryDocumentSnapshot ->
                                var documentId = queryDocumentSnapshot.data.getValue("diaryId")
                                diaryDB.document("$documentId")
                                    .update("blockedBy", (FieldValue.arrayRemove("$userId")))
                                    .addOnSuccessListener {
                                        if (index == documents.size() - 1) {
                                            var goBlockUserList =
                                                Intent(context, BlockUserListActivity::class.java)
                                            goBlockUserList.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                           context.startActivity(goBlockUserList)
                                        }
                                    }
                            }
                        }
                }

        }

        holder.itemView.likePersonAvatar.setOnClickListener { view ->
            var goEnlargeAvatar = Intent(context, EnlargeAvatarActivity::class.java)
            goEnlargeAvatar.putExtra("userAvatar", likePersonList[position].userAvatar)
            context.startActivity(goEnlargeAvatar)
        }
    }

    override fun getItemCount(): Int = likePersonList.size
}