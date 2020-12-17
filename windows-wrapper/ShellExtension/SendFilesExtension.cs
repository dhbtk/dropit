using SharpShell.Attributes;
using SharpShell.SharpContextMenu;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace ShellExtension
{
    [ComVisible(true)]
    [COMServerAssociation(AssociationType.AllFiles)]
    public class SendFilesExtension : SharpContextMenu
    {
        protected override bool CanShowMenu()
        {
            return true;
        }

        protected override ContextMenuStrip CreateMenu()
        {
            var menu = new ContextMenuStrip();

            var sendFilesItem = new ToolStripMenuItem
            {
                Text = "Send to phone",
                Image = Properties.Resources.desktop.ToBitmap(),
                ImageScaling = ToolStripItemImageScaling.SizeToFit
            };

            menu.Items.Add(sendFilesItem);

            return menu;
        }
    }
}
