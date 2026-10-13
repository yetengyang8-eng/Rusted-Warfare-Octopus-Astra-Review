"""Display-only clock provenance; no desktop/game operation."""
import io,json,sys,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
from human_observer import LiveObserver

class NativeClockTests(unittest.TestCase):
    def make(self,path):
        (path/'human-launch.json').write_text(json.dumps({'sessionId':'s','cli':{'port':47693}}),encoding='utf-8')
        return LiveObserver(path,True)
    def test_exact_native_time_and_frame_and_html(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp);o=self.make(root)
            packet=json.dumps({'status':'running','sessionId':'s','gameTimeMs':822600,'frame':245731})
            with patch('human_observer.urllib.request.urlopen',side_effect=[io.StringIO(packet),io.StringIO(packet)]) as fetch:
                o.poll_bridge_clock();self.assertEqual(o.clock_header(),'Game 13:42.6 | Frame 245731')
                fetch.assert_called_once_with('http://127.0.0.1:47693/state',timeout=0.3)
                o.last_clock_poll=-1;o.poll_bridge_clock();self.assertEqual(o.clock_header(),'Game 13:42.6 | Frame 245731')
            with patch('builtins.print'):o.display(True)
            html=(root/'rw-agent-live.html').read_text(encoding='utf-8')
            self.assertIn("id='native-clock'",html);self.assertIn('Game 13:42.6 | Frame 245731',html)
    def test_unknown_on_session_change_or_disconnect_no_wall_extrapolation(self):
        with tempfile.TemporaryDirectory() as temp:
            o=self.make(Path(temp))
            for sample in ({'status':'running','sessionId':'other','gameTimeMs':999,'frame':9},
                           {'status':'running','sessionId':'s','gameTimeMs':True,'frame':9}):
                o.last_clock_poll=-1
                with patch('human_observer.urllib.request.urlopen',return_value=io.StringIO(json.dumps(sample))):o.poll_bridge_clock()
                self.assertIn('Game UNKNOWN | Frame UNKNOWN',o.clock_header())
            o.last_clock_poll=-1
            with patch('human_observer.urllib.request.urlopen',side_effect=OSError('offline')):o.poll_bridge_clock()
            self.assertIn('Game UNKNOWN | Frame UNKNOWN',o.clock_header())
    def test_no_config_no_network_or_inferred_journal_clock(self):
        with tempfile.TemporaryDirectory() as temp:
            o=LiveObserver(temp);o.state.values['gameTime']=60000
            with patch('human_observer.urllib.request.urlopen') as fetch:o.poll_bridge_clock();fetch.assert_not_called()
            self.assertIn('Game UNKNOWN | Frame UNKNOWN',o.clock_header())
if __name__=='__main__':unittest.main()
