using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data;
using System.Diagnostics;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using System.Windows;

namespace DropIt
{
    /// <summary>
    /// Interação lógica para App.xaml
    /// </summary>
    public partial class App : Application
    {
        private void Application_Startup(object sender, StartupEventArgs e)
        {
            var jni = new JNI.JavaNativeInterface();
            var opts = new Dictionary<string, string>
            {
                ["-Djava.class.path"] = "dropit.jar",
                ["-Ddropit.exefile"] = Process.GetCurrentProcess().MainModule.FileName
            };
            jni.LoadVM(opts);
            jni.CallMainMethod("dropit/ApplicationKt", e.Args);
            Environment.Exit(0);
        }
    }

    internal static class ConsoleAllocator
    {
        [DllImport(@"kernel32.dll", SetLastError = true)]
        static extern bool AllocConsole();

        [DllImport(@"kernel32.dll")]
        static extern IntPtr GetConsoleWindow();

        [DllImport(@"user32.dll")]
        static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        const int SwHide = 0;
        const int SwShow = 5;


        public static void ShowConsoleWindow()
        {
            var handle = GetConsoleWindow();

            if (handle == IntPtr.Zero)
            {
                AllocConsole();
            }
            else
            {
                ShowWindow(handle, SwShow);
            }
        }

        public static void HideConsoleWindow()
        {
            var handle = GetConsoleWindow();

            ShowWindow(handle, SwHide);
        }
    }
}
