import { CameraView } from 'expo-camera';
import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';
import { Linking, Platform, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { NavEvent } from './src/core/machine';
import { mapData, markersData, placeName, useNova } from './src/useNova';

const C = { bg: '#000000', fg: '#FFFFFF', btn: '#FFD400', btnText: '#000000', dim: '#C8C8C8', danger: '#FF8A80' };

function BigButton(props: { label: string; hint?: string; onPress: () => void; secondary?: boolean }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={props.label}
      accessibilityHint={props.hint}
      onPress={props.onPress}
      style={({ pressed }) => [s.btn, props.secondary && s.btnSecondary, pressed && { opacity: 0.7 }]}
    >
      <Text style={[s.btnText, props.secondary && { color: C.fg }]}>{props.label}</Text>
    </Pressable>
  );
}

const Heading = ({ children }: { children: string }) => (
  <Text accessibilityRole="header" style={s.h1}>{children}</Text>
);

export default function App() {
  const { view, dispatch, chooseDestination } = useNova();
  const [team, setTeam] = useState(true);
  const send = (e: NavEvent) => () => dispatch(e);
  const guiding = view.state === 'WAITING' || view.state === 'GUIDING';

  return (
    <View style={s.root}>
      <StatusBar style="light" />
      <ScrollView contentContainerStyle={s.scroll}>
        <Heading>NOVA</Heading>

        {view.state === 'LOADING' && <Text style={s.body}>Loading.</Text>}

        {(view.state === 'HOME' || view.state === 'PERMISSION') && (
          <>
            <Text style={s.body}>Where do you want to go?</Text>
            {mapData.destinations.map((d) => (
              <BigButton
                key={d}
                label={`Go to ${placeName(d)}`}
                hint="Starts voice guidance"
                onPress={() => chooseDestination(d)}
              />
            ))}
            {view.showSettings && (
              <BigButton label="Open settings to allow the camera" onPress={() => Linking.openSettings()} secondary />
            )}
            <View style={s.row}>
              <Text style={[s.body, { flex: 1 }]} accessibilityElementsHidden importantForAccessibility="no">
                Team mode
              </Text>
              <Switch
                value={team}
                onValueChange={setTeam}
                accessibilityLabel="Team mode. Shows buttons to pretend a marker was seen."
              />
            </View>
          </>
        )}

        {guiding && (
          <>
            <Text accessibilityLiveRegion="polite" style={s.body}>
              {`Going to ${placeName(view.destinationId ?? '')}. ${
                view.state === 'WAITING' ? 'Waiting for the first marker.' : 'Guiding.'
              }`}
            </Text>
            {view.cameraOn && !view.cameraPaused && (
              <View style={s.camBox} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
                <CameraView
                  style={StyleSheet.absoluteFill}
                  facing="back"
                  onMountError={(e) => dispatch({ type: 'ErrorOccurred', kind: 'CAMERA', message: `Camera problem. ${e.message}` })}
                />
                <Text style={s.camTag}>Camera on (markers are not read in this version)</Text>
              </View>
            )}
            {view.lastInstruction && <Text style={s.body}>{view.lastInstruction}</Text>}
            <BigButton label="Repeat" onPress={send({ type: 'RepeatPressed' })} />
            <BigButton label="Where am I" onPress={send({ type: 'WhereAmIPressed' })} />
            <BigButton label="Stop" onPress={send({ type: 'StopPressed' })} secondary />
            {team && <TeamPanel dispatch={dispatch} />}
          </>
        )}

        {view.state === 'ARRIVED' && (
          <>
            <Text accessibilityLiveRegion="polite" style={s.body}>
              {`You have arrived at ${placeName(view.destinationId ?? '')}.`}
            </Text>
            <BigButton label="Choose another place" onPress={send({ type: 'ChooseAnotherPressed' })} />
          </>
        )}

        {view.state === 'ERROR' && (
          <>
            <Text accessibilityLiveRegion="assertive" style={[s.body, { color: C.danger }]}>
              {view.error?.message ?? 'Something went wrong.'}
            </Text>
            <BigButton label="Retry" onPress={send({ type: 'RetryPressed' })} />
            <BigButton label="Back" onPress={send({ type: 'BackPressed' })} secondary />
          </>
        )}

        {team && view.state !== 'LOADING' && (
          <View style={s.log} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
            <Text style={s.logTitle}>Team log (newest first)</Text>
            {view.lines.map((l, i) => (
              <Text key={i} style={s.logLine}>{l}</Text>
            ))}
          </View>
        )}
      </ScrollView>
    </View>
  );
}

/** Team mode: pretend the camera just read a marker. This replaces ArUco reading in the Expo version. */
function TeamPanel({ dispatch }: { dispatch: (e: NavEvent) => void }) {
  return (
    <View>
      <Text accessibilityRole="header" style={s.h2}>Team mode: markers</Text>
      <Text style={s.dim}>Press a marker as if the camera had just read it.</Text>
      {markersData.markers.map((m) => (
        <BigButton
          key={m.id}
          label={`Marker ${m.id}, ${placeName(m.nodeId)}`}
          hint={m.where}
          secondary
          onPress={() => dispatch({ type: 'Sighting', markerId: m.id })}
        />
      ))}
    </View>
  );
}

const s = StyleSheet.create({
  root: { flex: 1, backgroundColor: C.bg, paddingTop: Platform.OS === 'android' ? 40 : 56 },
  scroll: { padding: 16, gap: 12, paddingBottom: 48 },
  h1: { color: C.fg, fontSize: 36, fontWeight: '700' },
  h2: { color: C.fg, fontSize: 26, fontWeight: '700', marginTop: 16 },
  body: { color: C.fg, fontSize: 22, lineHeight: 30 },
  dim: { color: C.dim, fontSize: 16, marginBottom: 8 },
  row: { flexDirection: 'row', alignItems: 'center', marginTop: 16 },
  btn: { minHeight: 72, backgroundColor: C.btn, borderRadius: 12, justifyContent: 'center', paddingHorizontal: 16, marginVertical: 4 },
  btnSecondary: { backgroundColor: '#1E1E1E', borderWidth: 2, borderColor: C.btn },
  btnText: { color: C.btnText, fontSize: 22, fontWeight: '700' },
  camBox: { height: 160, borderRadius: 12, overflow: 'hidden', backgroundColor: '#111' },
  camTag: { position: 'absolute', bottom: 6, left: 8, right: 8, color: C.fg, fontSize: 12, backgroundColor: '#000A' },
  log: { marginTop: 24, padding: 8, borderWidth: 1, borderColor: '#444', borderRadius: 8 },
  logTitle: { color: C.dim, fontSize: 14, fontWeight: '700', marginBottom: 4 },
  logLine: { color: C.dim, fontSize: 12, marginVertical: 1 },
});
